package com.liferay.oauthbff.panel;

import com.liferay.application.list.BasePanelApp;
import com.liferay.application.list.PanelApp;
import com.liferay.object.model.ObjectDefinition;
import com.liferay.portal.kernel.model.Portlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Marcel Tanuri
 */
@Component(
        immediate = true,
        property = {
                "panel.app.order:Integer=10",
                "panel.category.key=category.oauthbff"
        },
        service = PanelApp.class
)
public class OAuthClientsPanelApp extends BasePanelApp {

    private ObjectDefinition objectDefinition;
    @Reference(
            target = "(javax.portlet.name=com.liferay.object.web.internal.object.entries.portlet.ObjectEntriesPortlet)"
    )
    private Portlet portlet;

    @Override
    public String getPortletId() {
        return objectDefinition.getPortletId(); // <- ESTA É A CHAVE
    }

    @Override
    public Portlet getPortlet() {
        return portlet;
    }

    @Reference(
            target = "(object.definition.name=OAuthClient)",
            unbind = "-"
    )
    public void setObjectDefinition(ObjectDefinition objectDefinition) {
        this.objectDefinition = objectDefinition;
    }
}
