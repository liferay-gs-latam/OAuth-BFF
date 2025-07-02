package com.liferay.oauthbff.token.request.factory;

import com.liferay.oauthbff.model.OAuthClient;
import com.liferay.oauthbff.token.request.model.TokenRequestInput;

/**
 * @author Marcel Tanuri
 */
public interface TokenRequestInputFactory {
    TokenRequestInput create(OAuthClient client);

    class UnauthorizedException extends RuntimeException {
        public UnauthorizedException(String message) {
            super(message);
        }
    }
}
