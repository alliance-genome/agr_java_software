package org.alliancegenome.agr_submission_client;

import javax.ws.rs.core.Response;

import org.alliancegenome.agr_submission.auth.Credentials;
import org.alliancegenome.agr_submission.interfaces.server.AuthenticationControllerInterface;

import si.mazi.rescu.RestProxyFactory;

public class AuthenticationControlerClientAPI implements AuthenticationControllerInterface {

    private AuthenticationControllerInterface api;
    
    public AuthenticationControlerClientAPI() { }
    
    public AuthenticationControlerClientAPI(String baseUrl) {
        api = RestProxyFactory.createProxy(AuthenticationControllerInterface.class, baseUrl);
    }
    
    @Override
    public Response authenticateUser(Credentials creds) {
        return api.authenticateUser(creds);
    }

}
