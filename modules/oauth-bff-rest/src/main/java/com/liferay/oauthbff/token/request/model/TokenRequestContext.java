package com.liferay.oauthbff.token.request.model;

/**
 * @author Marcel Tanuri
 */
public class TokenRequestContext {
    private long companyId;

    public TokenRequestContext() {}

    public TokenRequestContext(long companyId) {
        this.companyId = companyId;
    }

    public long getCompanyId() { return companyId; }
    public void setCompanyId(long companyId) { this.companyId = companyId; }
}