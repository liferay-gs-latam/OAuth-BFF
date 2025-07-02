package com.liferay.oauthbff.auth;

import com.liferay.oauthbff.model.OAuthClient;
import com.liferay.oauthbff.token.request.model.TokenRequestContext;

public interface AuthenticationStrategy {
    boolean supports(String authType);
    String getAuthorizationHeader(OAuthClient client, TokenRequestContext context);
}
