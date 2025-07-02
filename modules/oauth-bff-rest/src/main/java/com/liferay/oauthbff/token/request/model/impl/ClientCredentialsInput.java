package com.liferay.oauthbff.token.request.model.impl;

import com.liferay.oauthbff.token.request.model.TokenRequestInput;

/**
 * @author Marcel Tanuri
 */
public class ClientCredentialsInput implements TokenRequestInput {
    private final String clientId;
    private final String clientSecret;
    private final String tokenEndpoint;

    public ClientCredentialsInput(String clientId, String clientSecret, String tokenEndpoint) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.tokenEndpoint = tokenEndpoint;
    }

    public String getClientId() { return clientId; }
    public String getClientSecret() { return clientSecret; }
    public String getTokenEndpoint() { return tokenEndpoint; }
}