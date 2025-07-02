package com.liferay.oauthbff.resolver.token.impl;

import com.liferay.oauthbff.resolver.token.TokenResolver;
import com.liferay.oauthbff.token.cache.CachedToken;
import com.liferay.oauthbff.token.cache.service.OAuthTokenCacheService;
import com.liferay.oauthbff.token.request.model.TokenRequestContext;
import com.liferay.oauthbff.token.request.model.impl.ClientCredentialsInput;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Optional;

/**
 * @author Marcel Tanuri
 */
@Component(
        service = TokenResolver.class,
        property = "resolver.type=client_credentials"
)
public class ClientCredentialsTokenResolver implements TokenResolver<ClientCredentialsInput> {

    private static final Log _log = LogFactoryUtil.getLog(ClientCredentialsTokenResolver.class);

    @Reference
    private OAuthTokenCacheService oAuthTokenCacheService;

    @Override
    public String resolve(ClientCredentialsInput input, TokenRequestContext context) {
        String providerKey = input.getClientId();
        String ownerType = "application";
        String ownerId = providerKey;

        Optional<CachedToken> cached = oAuthTokenCacheService.getCachedToken(providerKey, ownerType, ownerId);
        if (cached.isPresent() && !cached.get().isExpired()) {
            _log.info("Reusing cached access token");
            return cached.get().getAccessToken();
        }

        _log.info(input.getClientId());
        _log.info(input.getClientSecret());

        try {
            String data = "grant_type=client_credentials" +
                    "&client_id=" + URLEncoder.encode(input.getClientId(), "UTF-8") +
                    "&client_secret=" + URLEncoder.encode(input.getClientSecret(), "UTF-8");

            URL url = new URL(input.getTokenEndpoint());
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(data.getBytes());
            }

            int status = conn.getResponseCode();
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    status < 400 ? conn.getInputStream() : conn.getErrorStream()
            ));
            StringBuilder responseBuilder = new StringBuilder();
            reader.lines().forEach(responseBuilder::append);
            String response = responseBuilder.toString();

            JSONObject json = JSONFactoryUtil.createJSONObject(response);
            String token = json.getString("access_token");
            long expiresIn = json.getLong("expires_in", 3600);
            String scope = json.getString("scope", "");

            if (token != null) {
                long expiresAt = System.currentTimeMillis() + (expiresIn * 1000);
                oAuthTokenCacheService.saveToken(providerKey, ownerType, ownerId, token, null, scope, expiresAt, true);
                _log.info("Token cached successfully. Token starts with: " + token.substring(0, Math.min(token.length(), 25)) + "...");
            } else {
                _log.warn("Access token not found in response: " + response);
            }

            return token;
        } catch (Exception e) {
            _log.error("Error while resolving client_credentials token", e);
            return null;
        }
    }
}