package com.liferay.oauthbff.resolver.token;

import com.liferay.oauthbff.token.request.model.TokenRequestContext;
import com.liferay.oauthbff.token.request.model.TokenRequestInput;

/**
 * @author Marcel Tanuri
 */
public interface TokenResolver<T extends TokenRequestInput> {
    String resolve(T input, TokenRequestContext context);
}
