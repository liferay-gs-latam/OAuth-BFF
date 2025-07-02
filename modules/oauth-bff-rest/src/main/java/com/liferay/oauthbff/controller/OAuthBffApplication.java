package com.liferay.oauthbff.controller;

import javax.ws.rs.core.Application;
import javax.ws.rs.ApplicationPath;

import org.osgi.service.component.annotations.Component;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Marcel Tanuri
 */
@Component(
        property = {
                "osgi.jaxrs.application.base=/oauth-bff",
                "osgi.jaxrs.name=OAuthBff.Rest",
                "liferay.auth.verifier=true",
                "liferay.oauth2=false"
        },
        service = Application.class
)
@ApplicationPath("/oauth-bff")
public class OAuthBffApplication extends Application {

        @Override
        public Set<Class<?>> getClasses() {
                Set<Class<?>> classes = new HashSet<>();
                classes.add(OAuthProxyController.class);
                return classes;
        }
}
