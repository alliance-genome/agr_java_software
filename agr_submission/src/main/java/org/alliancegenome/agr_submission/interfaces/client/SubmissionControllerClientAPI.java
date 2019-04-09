package org.alliancegenome.agr_submission.interfaces.client;

import org.alliancegenome.agr_submission.interfaces.server.SubmissionControllerInterface;
import org.alliancegenome.agr_submission.responces.APIResponce;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

import si.mazi.rescu.RestProxyFactory;

public class SubmissionControllerClientAPI implements SubmissionControllerInterface {

    private SubmissionControllerInterface api;
    
    public SubmissionControllerClientAPI() { }
    
    public SubmissionControllerClientAPI(String baseUrl) {
        api = RestProxyFactory.createProxy(SubmissionControllerInterface.class, baseUrl);
    }
    
    @Override
    public APIResponce getSnapShot(String releaseVersion) {
        return api.getSnapShot(releaseVersion);
    }

    @Override
    public APIResponce takeSnapShot(String api_access_token, String releaseVersion) {
        return api.takeSnapShot(api_access_token, releaseVersion);
    }

    @Override
    public APIResponce getReleases() {
        return api.getReleases();
    }

    @Override
    public APIResponce submitData(String api_access_token, MultipartFormDataInput input) {
        return api.submitData(api_access_token, input);
    }

    @Override
    public APIResponce validateData(MultipartFormDataInput input) {
        return api.validateData(input);
    }

}
