package com.liferay.oauthbff.token.request.factory.impl;

import com.liferay.oauthbff.model.OAuthClient;
import com.liferay.oauthbff.token.request.factory.TokenRequestInputFactory;
import com.liferay.oauthbff.token.request.factory.TokenRequestInputFactoryStrategy;
import com.liferay.oauthbff.token.request.model.TokenRequestInput;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.util.List;

/**
 * @author Marcel Tanuri
 */
@Component(service = TokenRequestInputFactory.class)
public class DefaultTokenRequestInputFactory implements TokenRequestInputFactory {

    @Reference
    private List<TokenRequestInputFactoryStrategy> strategies;

    public TokenRequestInput create(OAuthClient client) {
        return strategies.stream()
                .filter(s -> s.supports(client.getType()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported client type: " + client.getType()))
                .create(client);
    }
}
