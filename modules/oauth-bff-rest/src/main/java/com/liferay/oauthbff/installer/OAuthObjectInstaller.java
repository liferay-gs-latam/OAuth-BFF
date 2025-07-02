package com.liferay.oauthbff.installer;

import com.liferay.list.type.model.ListTypeDefinition;
import com.liferay.list.type.service.ListTypeDefinitionLocalService;
import com.liferay.list.type.service.ListTypeEntryLocalService;
import com.liferay.object.model.ObjectDefinition;
import com.liferay.object.model.ObjectFieldSetting;
import com.liferay.object.service.ObjectDefinitionLocalService;
import com.liferay.object.service.ObjectFieldLocalService;
import com.liferay.portal.kernel.cluster.ClusterExecutor;
import com.liferay.portal.kernel.cluster.ClusterNode;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.json.JSONFactory;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.model.Company;
import com.liferay.portal.kernel.service.CompanyLocalService;
import com.liferay.portal.kernel.service.UserLocalService;
import com.liferay.portal.kernel.util.LocaleUtil;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Marcel Tanuri
 */
@Component(immediate = true, service = OAuthObjectInstaller.class)
public class OAuthObjectInstaller {

    public static final String PANEL_APP_ORDER = "100";
    public static final String CATEGORY_OAUTHBFF = "category.oauthbff";
    public static final String SCOPE = "company";
    public static final String STORAGE_TYPE = "default";
    public static final int OBJECT_FOLDER_ID = 0;
    public static final String JSON_DEFINITION_PATH = "objects/";
    @Reference
    private ClusterExecutor clusterExecutor;
    @Reference
    private CompanyLocalService companyLocalService;
    @Reference
    private UserLocalService userLocalService;
    @Reference
    private ListTypeDefinitionLocalService listTypeDefinitionLocalService;
    @Reference
    private ListTypeEntryLocalService listTypeEntryLocalService;
    @Reference
    private ObjectDefinitionLocalService objectDefinitionLocalService;
    @Reference
    private ObjectFieldLocalService objectFieldLocalService;
    @Reference
    private com.liferay.object.service.persistence.ObjectFieldSettingPersistence objectFieldSettingPersistence;

    @Reference
    private JSONFactory jsonFactory;

    private long userId;
    private long companyId;

    @Activate
    public void activate() {
        try {
            if (clusterExecutor.isEnabled() && !isMasterNode()) return;

            Company company = companyLocalService.getCompanies().get(0);
            companyId = company.getCompanyId();
            userId = userLocalService.getDefaultUserId(companyId);

            installPicklist("oauth_client_type_picklist.json");
            installPicklist("oauth_owner_type_picklist.json");

            installObjectDefinition("oauth_client_object_definition.json");
            installObjectDefinition("oauth_token_object_definition.json");

        } catch (Exception e) {
            throw new RuntimeException("Erro ao instalar objetos OAuth", e);
        }
    }

    private void installPicklist(String resourcePath) throws Exception {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(JSON_DEFINITION_PATH + resourcePath)) {
            if (inputStream == null)
                throw new IllegalArgumentException("Recurso não encontrado: " + JSON_DEFINITION_PATH + resourcePath);

            String json = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            var jsonObject = jsonFactory.createJSONObject(json);

            String externalReferenceCode = jsonObject.getString("externalReferenceCode");
            ListTypeDefinition existing = listTypeDefinitionLocalService.fetchListTypeDefinitionByExternalReferenceCode(externalReferenceCode, companyId);
            if (existing != null) return;

            ListTypeDefinition definition = listTypeDefinitionLocalService.addListTypeDefinition(externalReferenceCode, userId, false);

            var entries = jsonObject.getJSONArray("listTypeEntries");
            for (int i = 0; i < entries.length(); i++) {
                var entry = entries.getJSONObject(i);
                listTypeEntryLocalService.addListTypeEntry(entry.getString("externalReferenceCode"), userId, definition.getListTypeDefinitionId(), entry.getString("key"), Map.of(LocaleUtil.US, entry.getString("name")));
            }
        }
    }

    private void installObjectDefinition(String resourcePath) throws Exception {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(JSON_DEFINITION_PATH + resourcePath)) {
            if (inputStream == null)
                throw new IllegalArgumentException("Recurso não encontrado: " + JSON_DEFINITION_PATH + resourcePath);

            String json = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            var jsonObject = jsonFactory.createJSONObject(json);

            String name = jsonObject.getString("name");

            ObjectDefinition existing = null;

            try {
                existing = objectDefinitionLocalService.getObjectDefinition(companyId, "C_" + name);
            } catch (PortalException e) {
                // log
            }

            if (existing != null) {
                return; // já existe, não tenta instalar de novo
            }

            ObjectDefinition objectDefinition = null;
            try {
                objectDefinition = objectDefinitionLocalService.addCustomObjectDefinition(
                        userId,
                        0,
                        "",
                        false, // enableComments
                        false, // enableFriendlyURLCustomization
                        false, // enableIndexSearch
                        false, // enableLocalization
                        false, // enableObjectEntryDraft
                        Map.of(LocaleUtil.US, name),
                        name,
                        "100",
                        "category.oauthbff",
                        Map.of(LocaleUtil.US, name + "s"),
                        false,
                        "company",
                        "default",
                        List.of()
                );

                var fields = jsonObject.getJSONArray("objectFields");
                for (int i = 0; i < fields.length(); i++) {
                    var field = fields.getJSONObject(i);
                    String filedName = field.getString("name");
                    String filedType = field.getString("type");
                    boolean required = field.has("required") && field.getBoolean("required");

                    if ("Picklist".equals(filedType)) {
                        String listERC = field.getString("listTypeDefinitionExternalReferenceCode");
                        addPicklistField(objectDefinition, filedName, listERC, required);
                    } else {
                        addBasicField(objectDefinition, field);
                    }
                }

                objectDefinitionLocalService.publishCustomObjectDefinition(userId, objectDefinition.getObjectDefinitionId());


            } catch (Exception ex) {
                if (objectDefinition != null) {
                    objectDefinitionLocalService.deleteObjectDefinition(objectDefinition);
                }
                throw new RuntimeException("Erro ao instalar ObjectDefinition " + name + ": " + ex.getMessage(), ex);
            }
        }
    }


    private void addBasicField(ObjectDefinition def, JSONObject field) throws Exception {
        String name = field.getString("name");
        String type = field.getString("type");
        boolean required = field.has("required") && field.getBoolean("required");

        List<ObjectFieldSetting> settings = new ArrayList<>();

        // Adiciona automaticamente o setting obrigatório para campos DateTime
        if ("DateTime".equals(type)) {
            ObjectFieldSetting timeStorageSetting = objectFieldSettingPersistence.create(0L);
            timeStorageSetting.setName("timeStorage");
            timeStorageSetting.setValue("explicit");
            settings.add(timeStorageSetting);
        }

        objectFieldLocalService.addCustomObjectField(
                null, userId, 0, def.getObjectDefinitionId(),
                type, type, false, false,
                null, Map.of(LocaleUtil.US, name),
                false, name, "false", null, required, false,
                settings
        );
    }


    private void addPicklistField(ObjectDefinition def, String name, String listERC, boolean required) throws Exception {
        long listTypeDefinitionId = listTypeDefinitionLocalService.getListTypeDefinitionByExternalReferenceCode(listERC, companyId).getListTypeDefinitionId();

        objectFieldLocalService.addCustomObjectField(null, userId, listTypeDefinitionId, def.getObjectDefinitionId(), "Picklist", "String", false, false, null, Map.of(LocaleUtil.US, name), false, name, "false", null, required, false, List.of());
    }

    private boolean isMasterNode() {
        if (!clusterExecutor.isEnabled()) return true;

        List<ClusterNode> nodes = clusterExecutor.getClusterNodes();
        ClusterNode localNode = clusterExecutor.getLocalClusterNode();

        if (localNode == null || nodes == null || nodes.isEmpty()) return true;

        nodes.sort((a, b) -> a.getClusterNodeId().compareTo(b.getClusterNodeId()));
        return localNode.equals(nodes.get(0));
    }
}
