package com.liferay.oauthbff.service.impl;

import com.liferay.oauthbff.auth.AuthenticationStrategy;
import com.liferay.oauthbff.model.OAuthClient;
import com.liferay.oauthbff.model.ProxyRequestContext;
import com.liferay.oauthbff.service.ProxyService;
import com.liferay.oauthbff.token.request.model.TokenRequestContext;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

@Component(service = ProxyService.class)
public class ProxyServiceImpl implements ProxyService {

    private static final Log _log = LogFactoryUtil.getLog(ProxyServiceImpl.class);

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    private final List<AuthenticationStrategy> authenticationStrategies = new ArrayList<>();

    @Override
    public Response forward(OAuthClient client, ProxyRequestContext ctx) throws Exception {
        TokenRequestContext tokenContext = new TokenRequestContext();
        String authHeader = resolveAuthHeader(client, tokenContext);

        URI targetUri = buildUri(client.getBaseURL(), ctx.getPath(), ctx.getQueryString());

        _log.info("Proxying request to: {" + targetUri + "}");

        HttpRequest.BodyPublisher bodyPublisher = createBodyPublisher(ctx.getBody());

        HttpRequest.Builder proxyHttpRequest = HttpRequest.newBuilder()
                .uri(targetUri)
                .method(ctx.getMethod(), bodyPublisher)
                .header("Authorization", authHeader)
                .header("Accept-Encoding", "gzip, deflate");

        copyHeaders(ctx, proxyHttpRequest);

        HttpResponse<InputStream> response = httpClient.send(
                proxyHttpRequest.build(),
                HttpResponse.BodyHandlers.ofInputStream()
        );

        InputStream responseStream = response.body();
        String encoding = response.headers().firstValue("Content-Encoding").orElse("");

        if ("gzip".equalsIgnoreCase(encoding)) {
            responseStream = new GZIPInputStream(responseStream);
        } else if ("deflate".equalsIgnoreCase(encoding)) {
            responseStream = new InflaterInputStream(responseStream);
        }

        String responseBody = new String(responseStream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);

        return Response.status(response.statusCode())
                .entity(responseBody)
                .type(response.headers().firstValue("Content-Type").orElse(MediaType.APPLICATION_JSON))
                .build();
    }

    private URI buildUri(String baseUrl, String path, String query) {
        return URI.create(baseUrl + (path.startsWith("/") ? path : "/" + path) + (query == null || query.isBlank() ? "" : "?" + query));
    }

    private String resolveAuthHeader(OAuthClient client, TokenRequestContext context) {
        return authenticationStrategies.stream()
                .filter(strategy -> strategy.supports(client.getType()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported auth type: " + client.getType()))
                .getAuthorizationHeader(client, context);
    }

    private HttpRequest.BodyPublisher createBodyPublisher(InputStream body) throws Exception {
        if (body == null) return HttpRequest.BodyPublishers.noBody();
        byte[] bytes = body.readAllBytes();
        return bytes.length > 0 ? HttpRequest.BodyPublishers.ofByteArray(bytes) : HttpRequest.BodyPublishers.noBody();
    }

    private void copyHeaders(ProxyRequestContext ctx, HttpRequest.Builder builder) {
        ctx.getHeaders().getRequestHeaders().entrySet().stream()
                .filter(entry -> !entry.getKey().equalsIgnoreCase("Authorization")
                        && !entry.getKey().equalsIgnoreCase("Host")
                        && !entry.getKey().equalsIgnoreCase("Connection"))
                .forEach(entry ->
                        entry.getValue().forEach(value ->
                                builder.header(entry.getKey(), value)
                        )
                );
    }

    protected void addAuthenticationStrategy(AuthenticationStrategy strategy) {
        authenticationStrategies.add(strategy);
    }

    protected void removeAuthenticationStrategy(AuthenticationStrategy strategy) {
        authenticationStrategies.remove(strategy);
    }
}
