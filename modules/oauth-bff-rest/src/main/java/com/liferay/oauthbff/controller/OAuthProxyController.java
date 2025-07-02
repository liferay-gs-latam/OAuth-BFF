package com.liferay.oauthbff.controller;

import com.liferay.oauthbff.model.OAuthClient;
import com.liferay.oauthbff.resolver.client.OAuthClientResolver;
import com.liferay.oauthbff.resolver.token.TokenResolver;
import com.liferay.oauthbff.resolver.token.TokenResolverRegistry;
import com.liferay.oauthbff.token.request.factory.TokenRequestInputFactory;
import com.liferay.oauthbff.token.request.model.TokenRequestContext;
import com.liferay.oauthbff.token.request.model.TokenRequestInput;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * @author Marcel Tanuri
 */
@Component(immediate = true, service = OAuthProxyController.class, property = {"osgi.jaxrs.application.select=(osgi.jaxrs.name=OAuthBff.Rest)", "osgi.jaxrs.resource=true"})
@Path("/proxy")
public class OAuthProxyController {

    private static final String CLIENT_ALIAS = "alias";
    private static final String PROXY_PATH = "proxyPath";
    private static final Log _log = LogFactoryUtil.getLog(OAuthProxyController.class);
    private final HttpClient httpClient = HttpClient.newHttpClient();
    @Reference
    private OAuthClientResolver oAuthClientResolver;
    @Reference
    private TokenResolverRegistry tokenResolverRegistry;
    @Reference
    private TokenRequestInputFactory tokenRequestInputFactory;

    private static String getQueryString(UriInfo uriInfo) {
        String queryString = uriInfo.getRequestUri().getQuery();

        if (queryString != null && queryString.contains("p_auth")) {
            queryString = queryString.replaceAll("(&|^)p_auth=[^&]*", "");
            queryString = queryString.replaceAll("^&", "");
        }

        return queryString;
    }

    @GET
    @Path("/ready")
    @Produces(MediaType.TEXT_PLAIN)
    public Response readinessCheck() {
        return Response.ok("OAuth BFF is ready").build();
    }

    @GET
    @Path("/{alias}/{proxyPath: .+}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response proxyGet(@PathParam(CLIENT_ALIAS) String alias, @PathParam(PROXY_PATH) String proxyPath, @Context HttpServletRequest request, @Context HttpHeaders headers, @Context UriInfo uriInfo) {
        return proxyRequest(alias, proxyPath, request, headers, uriInfo, null);
    }

    @POST
    @Path("/{alias}/{proxyPath: .+}")
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    public Response proxyPost(@PathParam(CLIENT_ALIAS) String alias, @PathParam(PROXY_PATH) String proxyPath, @Context HttpServletRequest request, @Context HttpHeaders headers, @Context UriInfo uriInfo, InputStream requestBody) {
        return proxyRequest(alias, proxyPath, request, headers, uriInfo, requestBody);
    }

    @PUT
    @Path("/{alias}/{proxyPath: .+}")
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    public Response proxyPut(@PathParam(CLIENT_ALIAS) String alias, @PathParam(PROXY_PATH) String proxyPath, @Context HttpServletRequest request, @Context HttpHeaders headers, @Context UriInfo uriInfo, InputStream requestBody) {
        return proxyRequest(alias, proxyPath, request, headers, uriInfo, requestBody);
    }

    @DELETE
    @Path("/{alias}/{proxyPath: .+}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response proxyDelete(@PathParam(CLIENT_ALIAS) String alias, @PathParam(PROXY_PATH) String proxyPath, @Context HttpServletRequest request, @Context HttpHeaders headers, @Context UriInfo uriInfo) {
        return proxyRequest(alias, proxyPath, request, headers, uriInfo, null);
    }

    @PATCH
    @Path("/{alias}/{proxyPath: .+}")
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    public Response proxyPatch(@PathParam(CLIENT_ALIAS) String alias, @PathParam(PROXY_PATH) String proxyPath, @Context HttpServletRequest request, @Context HttpHeaders headers, @Context UriInfo uriInfo, InputStream requestBody) {
        return proxyRequest(alias, proxyPath, request, headers, uriInfo, requestBody);
    }

    private Response proxyRequest(String alias, String proxyPath, HttpServletRequest request, HttpHeaders headers, UriInfo uriInfo, InputStream requestBody) {
        try {
            OAuthClient oAuthClient = oAuthClientResolver.resolve(alias);

            boolean allowed = isPathAllowed(oAuthClient, proxyPath);

            if (!allowed) {
                _log.warn("Blocked proxy request to disallowed path: " + proxyPath);
                return Response.status(Response.Status.FORBIDDEN).entity("{\"error\": \"Access to this endpoint is not allowed.\"}").type(MediaType.APPLICATION_JSON).build();
            }

            TokenRequestInput input = tokenRequestInputFactory.create(oAuthClient);
            String accessToken = resolveToken(input, new TokenRequestContext());

            String queryString = getQueryString(uriInfo);
            URI targetUri = UriBuilder.fromUri(oAuthClient.getBaseURL()).path(proxyPath).replaceQuery(queryString).build();

            _log.info("Proxying request to: {" + targetUri + "}");

            HttpRequest.BodyPublisher bodyPublisher = createBodyPublisher(requestBody);

            HttpRequest.Builder proxyHttpRequest = HttpRequest.newBuilder().uri(targetUri).method(request.getMethod(), bodyPublisher).header("Authorization", "Bearer " + accessToken);

            copyHeaders(headers, proxyHttpRequest);

            HttpResponse<String> response = httpClient.send(proxyHttpRequest.build(), HttpResponse.BodyHandlers.ofString());

            return Response.status(response.statusCode()).entity(response.body()).type(response.headers().firstValue("Content-Type").orElse(MediaType.APPLICATION_JSON)).build();

        } catch (TokenRequestInputFactory.UnauthorizedException e) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("{\"error\": \"" + e.getMessage() + "\"}").type(MediaType.APPLICATION_JSON).build();
        } catch (Exception e) {
            _log.error("Error during proxy request", e);
            return Response.serverError().entity("{\"error\": \"Unexpected error: " + e.getMessage() + "\"}").type(MediaType.APPLICATION_JSON).build();
        }
    }

    private HttpRequest.BodyPublisher createBodyPublisher(InputStream requestBody) throws IOException {
        if (requestBody == null) {
            return HttpRequest.BodyPublishers.noBody();
        }

        byte[] bodyBytes = requestBody.readAllBytes();

        return (bodyBytes.length > 0) ? HttpRequest.BodyPublishers.ofByteArray(bodyBytes) : HttpRequest.BodyPublishers.noBody();
    }

    private void copyHeaders(HttpHeaders headers, HttpRequest.Builder proxyHttpRequest) {
        headers.getRequestHeaders().entrySet().stream().filter(entry -> !entry.getKey().equalsIgnoreCase("Authorization") && !entry.getKey().equalsIgnoreCase("Host") && !entry.getKey().equalsIgnoreCase("connection")).forEach(entry -> entry.getValue().forEach(value -> proxyHttpRequest.header(entry.getKey(), value)));
    }

    private <T extends TokenRequestInput> String resolveToken(T input, TokenRequestContext context) {
        TokenResolver<T> resolver = tokenResolverRegistry.getResolver(input);
        return resolver.resolve(input, context);
    }

    private boolean isPathAllowed(OAuthClient client, String proxyPath) {
        String normalizedPath = proxyPath.startsWith("/") ? proxyPath : "/" + proxyPath;

        return client.getAllowedEndpoints().stream().map(endpoint -> endpoint.startsWith("/") ? endpoint : "/" + endpoint).anyMatch(allowed -> normalizedPath.equals(allowed) || normalizedPath.startsWith(allowed + "/"));
    }

}