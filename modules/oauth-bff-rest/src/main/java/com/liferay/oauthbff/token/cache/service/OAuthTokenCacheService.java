package com.liferay.oauthbff.token.cache.service;

import com.liferay.oauthbff.token.cache.CachedToken;

import java.util.Optional;

/**
 * @author Marcel Tanuri
 */
public interface OAuthTokenCacheService {
    Optional<CachedToken> getCachedToken(String providerKey, String ownerType, String ownerId);
    void saveToken(String providerKey, String ownerType, String ownerId, String accessToken, String refreshToken, String scope, long expiresAt, boolean reuseEnabled);
}
