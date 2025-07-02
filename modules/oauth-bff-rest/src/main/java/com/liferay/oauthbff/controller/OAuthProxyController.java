package com.liferay.oauthbff.controller;

import com.liferay.oauthbff.model.OAuthClient;
import com.liferay.oauthbff.model.ProxyRequestContext;
import com.liferay.oauthbff.resolver.client.OAuthClientResolver;
import com.liferay.oauthbff.service.ProxyService;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.io.InputStream;

@Component(
        immediate = true,
        service = OAuthProxyController.class,
        property = {
                "osgi.jaxrs.application.select=(osgi.jaxrs.name=OAuthBff.Rest)",
                "osgi.jaxrs.resource=true"
        }
)
@Path("/proxy")
public class OAuthProxyController {

    private static final String CLIENT_ALIAS = "alias";
    private static final String PROXY_PATH = "proxyPath";
    private static final Log _log = LogFactoryUtil.getLog(OAuthProxyController.class);

    @Reference
    private OAuthClientResolver oAuthClientResolver;

    @Reference
    private ProxyService proxyService;

    @GET
    @Path("/ready")
    @Produces(MediaType.TEXT_PLAIN)
    public Response readinessCheck() {
        return Response.ok("OAuth BFF is ready").build();
    }

    @GET
    @Path("/{alias}/{proxyPath: .+}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response proxyGet(
            @PathParam(CLIENT_ALIAS) String alias,
            @PathParam(PROXY_PATH) String proxyPath,
            @Context HttpServletRequest request,
            @Context HttpHeaders headers,
            @Context UriInfo uriInfo
    ) {
        return proxyRequest("GET", alias, proxyPath, request, headers, uriInfo, null);
    }

    @POST
    @Path("/{alias}/{proxyPath: .+}")
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    public Response proxyPost(
            @PathParam(CLIENT_ALIAS) String alias,
            @PathParam(PROXY_PATH) String proxyPath,
            @Context HttpServletRequest request,
            @Context HttpHeaders headers,
            @Context UriInfo uriInfo,
            InputStream requestBody
    ) {
        return proxyRequest("POST", alias, proxyPath, request, headers, uriInfo, requestBody);
    }

    @PUT
    @Path("/{alias}/{proxyPath: .+}")
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    public Response proxyPut(
            @PathParam(CLIENT_ALIAS) String alias,
            @PathParam(PROXY_PATH) String proxyPath,
            @Context HttpServletRequest request,
            @Context HttpHeaders headers,
            @Context UriInfo uriInfo,
            InputStream requestBody
    ) {
        return proxyRequest("PUT", alias, proxyPath, request, headers, uriInfo, requestBody);
    }

    @DELETE
    @Path("/{alias}/{proxyPath: .+}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response proxyDelete(
            @PathParam(CLIENT_ALIAS) String alias,
            @PathParam(PROXY_PATH) String proxyPath,
            @Context HttpServletRequest request,
            @Context HttpHeaders headers,
            @Context UriInfo uriInfo
    ) {
        return proxyRequest("DELETE", alias, proxyPath, request, headers, uriInfo, null);
    }

    @PATCH
    @Path("/{alias}/{proxyPath: .+}")
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    public Response proxyPatch(
            @PathParam(CLIENT_ALIAS) String alias,
            @PathParam(PROXY_PATH) String proxyPath,
            @Context HttpServletRequest request,
            @Context HttpHeaders headers,
            @Context UriInfo uriInfo,
            InputStream requestBody
    ) {
        return proxyRequest("PATCH", alias, proxyPath, request, headers, uriInfo, requestBody);
    }

    private Response proxyRequest(
            String method,
            String alias,
            String proxyPath,
            HttpServletRequest request,
            HttpHeaders headers,
            UriInfo uriInfo,
            InputStream requestBody
    ) {
        try {
            OAuthClient client = oAuthClientResolver.resolve(alias);

            if (!isPathAllowed(client, proxyPath)) {
                _log.warn("Blocked proxy request to disallowed path: " + proxyPath);
                return Response.status(Response.Status.FORBIDDEN)
                        .entity("{\"error\": \"Access to this endpoint is not allowed.\"}")
                        .type(MediaType.APPLICATION_JSON)
                        .build();
            }

            ProxyRequestContext ctx = new ProxyRequestContext.Builder()
                    .method(method)
                    .path(proxyPath)
                    .queryString(getQueryString(uriInfo))
                    .headers(headers)
                    .body(requestBody)
                    .servletRequest(request)
                    .uriInfo(uriInfo)
                    .build();

            return proxyService.forward(client, ctx);

        } catch (Exception e) {
            _log.error("Error during proxy forwarding", e);
            return Response.serverError()
                    .entity("{\"error\": \"Unexpected error: " + e.getMessage() + "\"}")
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }

    private boolean isPathAllowed(OAuthClient client, String proxyPath) {
        String normalizedPath = proxyPath.startsWith("/") ? proxyPath : "/" + proxyPath;

        return client.getAllowedEndpoints().stream()
                .map(endpoint -> endpoint.startsWith("/") ? endpoint : "/" + endpoint)
                .anyMatch(allowed -> normalizedPath.equals(allowed) || normalizedPath.startsWith(allowed + "/"));
    }

    private String getQueryString(UriInfo uriInfo) {
        String queryString = uriInfo.getRequestUri().getQuery();

        if (queryString != null && queryString.contains("p_auth")) {
            queryString = queryString.replaceAll("(&|^)p_auth=[^&]*", "");
            queryString = queryString.replaceAll("^&", "");
        }

        return queryString;
    }
}
