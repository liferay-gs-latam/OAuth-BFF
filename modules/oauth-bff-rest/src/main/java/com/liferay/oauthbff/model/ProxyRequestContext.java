package com.liferay.oauthbff.model;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;
import java.io.InputStream;

public class ProxyRequestContext {

    private final String method;
    private final String path;
    private final String queryString;
    private final HttpHeaders headers;
    private final InputStream body;
    private final HttpServletRequest servletRequest;
    private final UriInfo uriInfo;

    private ProxyRequestContext(Builder builder) {
        this.method = builder.method;
        this.path = builder.path;
        this.queryString = builder.queryString;
        this.headers = builder.headers;
        this.body = builder.body;
        this.servletRequest = builder.servletRequest;
        this.uriInfo = builder.uriInfo;
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public String getQueryString() {
        return queryString;
    }

    public HttpHeaders getHeaders() {
        return headers;
    }

    public InputStream getBody() {
        return body;
    }

    public HttpServletRequest getServletRequest() {
        return servletRequest;
    }

    public UriInfo getUriInfo() {
        return uriInfo;
    }

    // 🔨 Fluent Builder
    public static class Builder {
        private String method;
        private String path;
        private String queryString;
        private HttpHeaders headers;
        private InputStream body;
        private HttpServletRequest servletRequest;
        private UriInfo uriInfo;

        public Builder method(String method) {
            this.method = method;
            return this;
        }

        public Builder path(String path) {
            this.path = path;
            return this;
        }

        public Builder queryString(String queryString) {
            this.queryString = queryString;
            return this;
        }

        public Builder headers(HttpHeaders headers) {
            this.headers = headers;
            return this;
        }

        public Builder body(InputStream body) {
            this.body = body;
            return this;
        }

        public Builder servletRequest(HttpServletRequest servletRequest) {
            this.servletRequest = servletRequest;
            return this;
        }

        public Builder uriInfo(UriInfo uriInfo) {
            this.uriInfo = uriInfo;
            return this;
        }

        public ProxyRequestContext build() {
            return new ProxyRequestContext(this);
        }
    }
}
