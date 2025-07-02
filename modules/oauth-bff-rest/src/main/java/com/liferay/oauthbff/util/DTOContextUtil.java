package com.liferay.oauthbff.util;

import com.liferay.portal.vulcan.dto.converter.DefaultDTOConverterContext;

import java.util.HashMap;
import java.util.Locale;

/**
 * @author Marcel Tanuri
 */
public class DTOContextUtil {

    public static DefaultDTOConverterContext contextWithDefaultUser(long companyId) {
        try {
            return new DefaultDTOConverterContext(
                    false,                 // acceptAllLanguages
                    new HashMap<>(),       // actions
                    null,                  // dtoConverterRegistry
                    null,                  // httpServletRequest
                    null,                  // id
                    Locale.getDefault(),   // locale
                    null,                  // uriInfo
                    null                   // user
            );
        } catch (Exception e) {
            throw new RuntimeException("Unable to obtain default user for companyId=" + companyId, e);
        }
    }
}
