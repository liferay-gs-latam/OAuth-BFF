package com.liferay.oauthbff.token.cache;

/**
 * @author Marcel Tanuri
 */
public class CachedToken {
    private final String accessToken;
    private final String refreshToken;
    private final String scope;
    private final long expiresAt;
    private final boolean reuseEnabled;

    public CachedToken(String accessToken, String refreshToken, String scope, long expiresAt, boolean reuseEnabled) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.scope = scope;
        this.expiresAt = expiresAt;
        this.reuseEnabled = reuseEnabled;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public String getScope() {
        return scope;
    }

    public long getExpiresAt() {
        return expiresAt;
    }

    public boolean isReuseEnabled() {
        return reuseEnabled;
    }

    public boolean isExpired() {
        long bufferMillis = 30 * 1000; // 30-second buffer
        return System.currentTimeMillis() > (expiresAt - bufferMillis);
    }
}
