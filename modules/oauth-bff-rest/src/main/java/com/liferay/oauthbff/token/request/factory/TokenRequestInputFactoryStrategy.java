package com.liferay.oauthbff.token.request.factory;

import com.liferay.oauthbff.model.OAuthClient;
import com.liferay.oauthbff.token.request.model.TokenRequestInput;

/**
 * @author Marcel Tanuri
 */
public interface TokenRequestInputFactoryStrategy {
    boolean supports(String type);
    TokenRequestInput create(OAuthClient client);
}
