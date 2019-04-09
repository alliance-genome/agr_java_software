package org.alliancegenome.agr_submission_client;

import java.util.List;

import org.alliancegenome.agr_submission.entities.ReleaseVersion;
import org.alliancegenome.agr_submission.interfaces.server.ReleaseVersionControllerInterface;

import si.mazi.rescu.RestProxyFactory;

public class ReleaseVersionControllerClientAPI implements ReleaseVersionControllerInterface {

    private ReleaseVersionControllerInterface api;
    
    public ReleaseVersionControllerClientAPI() { }
    
    public ReleaseVersionControllerClientAPI(String baseUrl) {
        api = RestProxyFactory.createProxy(ReleaseVersionControllerInterface.class, baseUrl);
    }
    
    @Override
    public ReleaseVersion create(ReleaseVersion entity) {
        return api.create(entity);
    }

    @Override
    public ReleaseVersion get(Long id) {
        return api.get(id);
    }

    @Override
    public ReleaseVersion update(ReleaseVersion entity) {
        return api.update(entity);
    }

    @Override
    public ReleaseVersion delete(Long id) {
        return api.delete(id);
    }

    @Override
    public List<ReleaseVersion> getDataTypes() {
        return api.getDataTypes();
    }

    @Override
    public ReleaseVersion addSchema(String release_id, String schema_id) {
        return api.addSchema(release_id, schema_id);
    }

}
