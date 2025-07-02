package com.liferay.oauthbff.token.request.factory.impl;

import com.liferay.oauthbff.model.OAuthClient;
import com.liferay.oauthbff.token.request.model.impl.OidcSessionInput;
import com.liferay.oauthbff.token.request.model.TokenRequestInput;
import com.liferay.oauthbff.token.request.factory.TokenRequestInputFactoryStrategy;
import com.liferay.portal.kernel.security.auth.PrincipalThreadLocal;
import org.osgi.service.component.annotations.Component;

/**
 * @author Marcel Tanuri
 */
@Component(service = TokenRequestInputFactoryStrategy.class)
public class OidcSessionInputFactoryStrategy implements TokenRequestInputFactoryStrategy {

    @Override
    public boolean supports(String type) {
        return "oidc_session".equalsIgnoreCase(type);
    }

    @Override
    public TokenRequestInput create(OAuthClient client) {
        long userId = PrincipalThreadLocal.getUserId();
        if (userId == 0) {
            throw new DefaultTokenRequestInputFactory.UnauthorizedException("User must be authenticated for OIDC session");
        }

        return new OidcSessionInput(
                userId,
                client.getClientId(),
                client.getAuthEndpoint()
        );
    }
}
