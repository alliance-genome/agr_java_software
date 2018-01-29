package org.alliancegenome.api.controller;

import javax.enterprise.context.RequestScoped;

import org.alliancegenome.shared.config.ConfigHelper;

@RequestScoped
public class BaseController {

    private ConfigHelper config = new ConfigHelper();

    /* if token is not defined in properties file, then do not require one.  Otherwise, must
     * be an exact match (case sensitive).
     */
    protected boolean authenticate(String api_access_token) {
        String accessToken = config.getApiAccessToken();
        if (accessToken != null) {
            return accessToken.equals(api_access_token);
        }
        return true;
    }

}
