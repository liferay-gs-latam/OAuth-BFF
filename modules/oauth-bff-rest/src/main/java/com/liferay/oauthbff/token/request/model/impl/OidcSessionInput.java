package com.liferay.oauthbff.token.request.model.impl;

import com.liferay.oauthbff.token.request.model.TokenRequestInput;

/**
 * @author Marcel Tanuri
 */
public class OidcSessionInput implements TokenRequestInput {
    private final long userId;
    private final String clientId;
    private final String wellKnownURI;

    public OidcSessionInput(long userId, String clientId, String wellKnownURI) {
        this.userId = userId;
        this.clientId = clientId;
        this.wellKnownURI = wellKnownURI;
    }

    public long getUserId() { return userId; }
    public String getClientId() { return clientId; }
    public String getWellKnownURI() { return wellKnownURI; }
}