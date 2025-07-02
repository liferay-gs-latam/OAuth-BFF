package com.liferay.oauthbff.service;

import com.liferay.oauthbff.model.OAuthClient;
import com.liferay.oauthbff.model.ProxyRequestContext;

import javax.ws.rs.core.Response;

public interface ProxyService {
    Response forward(OAuthClient client, ProxyRequestContext context) throws Exception;
}
