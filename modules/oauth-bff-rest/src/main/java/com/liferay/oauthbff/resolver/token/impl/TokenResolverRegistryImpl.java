package com.liferay.oauthbff.resolver.token.impl;

import com.liferay.oauthbff.resolver.token.TokenResolver;
import com.liferay.oauthbff.resolver.token.TokenResolverRegistry;
import com.liferay.oauthbff.token.request.model.TokenRequestInput;
import com.liferay.oauthbff.token.request.model.impl.ClientCredentialsInput;
import com.liferay.oauthbff.token.request.model.impl.OidcSessionInput;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Marcel Tanuri
 */
@Component(service = TokenResolverRegistry.class)
public class TokenResolverRegistryImpl implements TokenResolverRegistry {

    @Reference(target = "(resolver.type=client_credentials)")
    private TokenResolver<ClientCredentialsInput> clientCredentialsTokenResolver;

    @Reference(target = "(resolver.type=client_credentials)")
    private TokenResolver<OidcSessionInput> oidcSessionTokenResolver;

    @Override
    @SuppressWarnings("unchecked")
    public <T extends TokenRequestInput> TokenResolver<T> getResolver(T input) {
        if (input instanceof ClientCredentialsInput) {
            return (TokenResolver<T>) clientCredentialsTokenResolver;
        } else if (input instanceof OidcSessionInput) {
            return (TokenResolver<T>) oidcSessionTokenResolver;
        }
        throw new IllegalArgumentException("Unsupported input type: " + input.getClass());
    }
}
