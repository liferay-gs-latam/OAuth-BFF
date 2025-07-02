package com.liferay.oauthbff.model;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Marcel Tanuri
 */
public class OAuthClient {
    private final String clientId;
    private final String clientSecret;
    private final String tokenEndpoint;
    private final String authEndpoint;
    private final String type;
    private final String baseURL;
    private final String allowedEndpoints;

    public OAuthClient(
            String clientId,
            String clientSecret,
            String tokenEndpoint,
            String authEndpoint,
            String type,
            String baseURL,
            String allowedEndpoints
    ) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.tokenEndpoint = tokenEndpoint;
        this.authEndpoint = authEndpoint;
        this.type = type;
        this.baseURL = baseURL;
        this.allowedEndpoints = allowedEndpoints;
    }

    public String getClientId() { return clientId; }
    public String getClientSecret() { return clientSecret; }
    public String getTokenEndpoint() { return tokenEndpoint; }
    public String getAuthEndpoint() { return authEndpoint; }
    public String getType() { return type; }
    public String getBaseURL() { return baseURL; }
    public String getAllowedEndpointsRaw() { return allowedEndpoints; }

    public List<String> getAllowedEndpoints() {
        if (allowedEndpoints == null || allowedEndpoints.isBlank()) {
            return List.of();
        }

        return Arrays.stream(allowedEndpoints.split("\\r?\\n"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    public boolean isEndpointAllowed(String proxyPath) {
        return getAllowedEndpoints().stream().anyMatch(proxyPath::startsWith);
    }
}
