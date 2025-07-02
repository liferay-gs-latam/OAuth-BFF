package com.liferay.oauthbff.resolver.token;

import com.liferay.oauthbff.token.request.model.TokenRequestInput;

/**
 * @author Marcel Tanuri
 */
public interface TokenResolverRegistry {
    <T extends TokenRequestInput> TokenResolver<T> getResolver(T input);
}
