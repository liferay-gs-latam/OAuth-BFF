package com.liferay.oauthbff.token.request.factory.impl;

import com.liferay.oauthbff.model.OAuthClient;
import com.liferay.oauthbff.token.request.factory.TokenRequestInputFactoryStrategy;
import com.liferay.oauthbff.token.request.model.TokenRequestInput;
import com.liferay.oauthbff.token.request.model.impl.ClientCredentialsInput;
import org.osgi.service.component.annotations.Component;

/**
 * @author Marcel Tanuri
 */
@Component(service = TokenRequestInputFactoryStrategy.class)
public class ClientCredentialsInputFactoryStrategy implements TokenRequestInputFactoryStrategy {

    @Override
    public boolean supports(String type) {
        return "clientCredentials".equalsIgnoreCase(type);
    }

    @Override
    public TokenRequestInput create(OAuthClient client) {
        return new ClientCredentialsInput(
                client.getClientId(),
                client.getClientSecret(),
                client.getTokenEndpoint()
        );
    }
}
