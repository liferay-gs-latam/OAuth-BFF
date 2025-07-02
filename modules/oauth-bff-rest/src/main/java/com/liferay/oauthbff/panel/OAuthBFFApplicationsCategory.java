package com.liferay.oauthbff.panel;

import com.liferay.application.list.BasePanelCategory;
import com.liferay.application.list.PanelCategory;
import com.liferay.application.list.constants.PanelCategoryKeys;
import org.osgi.service.component.annotations.Component;

import java.util.Locale;

/**
 * @author Marcel Tanuri
 */
@Component(
        immediate = true,
        property = {
                "panel.category.key=" + PanelCategoryKeys.APPLICATIONS_MENU,
                "panel.category.order:Integer=200"
        },
        service = PanelCategory.class
)
public class OAuthBFFApplicationsCategory extends BasePanelCategory {

    @Override
    public String getKey() {
        return "category.oauthbff";
    }

    @Override
    public String getLabel(Locale locale) {
        return "OAuth BFF";
    }
}
