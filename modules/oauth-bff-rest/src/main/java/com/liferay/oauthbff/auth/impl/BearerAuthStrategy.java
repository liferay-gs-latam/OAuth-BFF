package com.liferay.oauthbff.auth.impl;

import com.liferay.oauthbff.auth.AuthenticationStrategy;
import com.liferay.oauthbff.model.OAuthClient;
import com.liferay.oauthbff.resolver.token.TokenResolverRegistry;
import com.liferay.oauthbff.token.request.factory.TokenRequestInputFactory;
import com.liferay.oauthbff.token.request.model.TokenRequestContext;
import com.liferay.oauthbff.token.request.model.TokenRequestInput;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = AuthenticationStrategy.class)
public class BearerAuthStrategy implements AuthenticationStrategy {

    @Reference
    private TokenResolverRegistry tokenResolverRegistry;

    @Reference
    private TokenRequestInputFactory tokenRequestInputFactory;

    @Override
    public boolean supports(String authType) {
        return "clientCredentials".equalsIgnoreCase(authType)
                || "authorizationCode".equalsIgnoreCase(authType)
                || "oidcSession".equalsIgnoreCase(authType);
    }
    
    @Override
    public String getAuthorizationHeader(OAuthClient client, TokenRequestContext context) {
        TokenRequestInput input = tokenRequestInputFactory.create(client);
        String token = tokenResolverRegistry.getResolver(input).resolve(input, context);
        return "Bearer " + token;
    }
}
