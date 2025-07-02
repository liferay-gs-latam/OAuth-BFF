package com.liferay.oauthbff.auth.impl;

import com.liferay.oauthbff.auth.AuthenticationStrategy;
import com.liferay.oauthbff.model.OAuthClient;
import com.liferay.oauthbff.token.request.model.TokenRequestContext;
import org.osgi.service.component.annotations.Component;

import java.util.Base64;

@Component(service = AuthenticationStrategy.class)
public class BasicAuthStrategy implements AuthenticationStrategy {

    @Override
    public boolean supports(String authType) {
        return "basicAuth".equalsIgnoreCase(authType);
    }

    @Override
    public String getAuthorizationHeader(OAuthClient client, TokenRequestContext context) {
        String credentials = client.getClientId() + ":" + client.getClientSecret();
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes());
    }
}
