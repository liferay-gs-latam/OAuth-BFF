package com.liferay.oauthbff.resolver.client;

import com.liferay.oauthbff.model.OAuthClient;

/**
 * @author Marcel Tanuri
 */
public interface OAuthClientResolver {
    OAuthClient resolve(String alias) throws Exception;
}
