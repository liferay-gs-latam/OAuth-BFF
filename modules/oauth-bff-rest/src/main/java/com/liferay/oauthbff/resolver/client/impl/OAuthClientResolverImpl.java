package com.liferay.oauthbff.resolver.client.impl;

import com.liferay.oauthbff.model.OAuthClient;
import com.liferay.oauthbff.resolver.client.OAuthClientResolver;
import com.liferay.oauthbff.util.DTOContextUtil;
import com.liferay.object.model.ObjectDefinition;
import com.liferay.object.rest.dto.v1_0.ObjectEntry;
import com.liferay.object.rest.manager.v1_0.ObjectEntryManager;
import com.liferay.petra.function.UnsafeSupplier;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.security.auth.CompanyThreadLocal;
import com.liferay.portal.vulcan.pagination.Page;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.util.Map;
import java.util.Optional;

/**
 * @author Marcel Tanuri
 */
@Component(service = OAuthClientResolver.class)
public class OAuthClientResolverImpl implements OAuthClientResolver {

    private static final String OAUTH_CLIENT = "C_OAuthClient";

    private final Log _log = LogFactoryUtil.getLog(OAuthClientResolverImpl.class);

    @Reference(target = "(object.entry.manager.storage.type=default)")
    private ObjectEntryManager _objectEntryManager;

    @Reference
    private com.liferay.object.service.ObjectDefinitionLocalService _objectDefinitionLocalService;

    @Override
    public OAuthClient resolve(String alias) throws Exception {
        long companyId = CompanyThreadLocal.getCompanyId();

        ObjectDefinition objectDefinition = _objectDefinitionLocalService.getObjectDefinition(companyId, OAUTH_CLIENT);

        if (objectDefinition == null) {
            throw new RuntimeException("OAuthClient object definition not found");
        }

        Page<ObjectEntry> objectEntries = _objectEntryManager.getObjectEntries(companyId, objectDefinition, null, null, DTOContextUtil.contextWithDefaultUser(companyId), "alias eq '" + alias + "' and enabled eq true", null, null, null);

        if (objectEntries.getItems().isEmpty()) {
            throw new RuntimeException("OAuthClient not found for alias: " + alias);
        }

        ObjectEntry objectEntry = objectEntries.getItems().stream().findFirst().orElseThrow(() -> new RuntimeException("No OAuthClient entry found for alias: " + alias));

        Map<String, Object> entry = objectEntry.getProperties();

        Optional<String> type = getTypeAttr(entry);

        if (type.isEmpty()) {
            throw new RuntimeException("Type not found for OAuthClient with alias: " + alias);
        }

        return new OAuthClient(
                (String) entry.get("clientId"),
                (String) entry.get("clientSecret"),
                (String) entry.get("tokenEndpoint"),
                (String) entry.get("authEndpoint"),
                type.get(),
                (String) entry.get("baseURL"),
                (String) entry.get("allowedEndpoints")
        );
    }

    private Optional<String> getTypeAttr(Map<String, Object> entry) {
        Optional<String> type = Optional.empty();
        Object typeObj = entry.get("type");

        if (typeObj != null) {
            try {
                Object resolved = null;

                if (typeObj instanceof UnsafeSupplier<?, ?> supplier) {
                    resolved = supplier.get();
                } else {
                    resolved = typeObj;
                }

                _log.info("Resolved picklist (class=" + resolved.getClass() + "): " + resolved);

                if (resolved != null) {
                    JSONObject json = JSONFactoryUtil.createJSONObject(resolved.toString());

                    type = Optional.ofNullable(json.getString("key", null));
                }
            } catch (Exception e) {
                _log.error("Erro ao resolver campo 'type'", e);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }

        return type;
    }
}
