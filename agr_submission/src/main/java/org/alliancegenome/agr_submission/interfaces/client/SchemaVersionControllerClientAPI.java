package org.alliancegenome.agr_submission.interfaces.client;

import java.util.List;

import org.alliancegenome.agr_submission.entities.SchemaVersion;
import org.alliancegenome.agr_submission.interfaces.server.SchemaVersionControllerInterface;

import si.mazi.rescu.RestProxyFactory;

public class SchemaVersionControllerClientAPI implements SchemaVersionControllerInterface {

    private SchemaVersionControllerInterface api;
    
    public SchemaVersionControllerClientAPI() { }
    
    public SchemaVersionControllerClientAPI(String baseUrl) {
        api = RestProxyFactory.createProxy(SchemaVersionControllerInterface.class, baseUrl);
    }
    
    @Override
    public SchemaVersion create(SchemaVersion entity) {
        return api.create(entity);
    }

    @Override
    public SchemaVersion get(Long id) {
        return api.get(id);
    }

    @Override
    public SchemaVersion update(SchemaVersion entity) {
        return api.update(entity);
    }

    @Override
    public SchemaVersion delete(Long id) {
        return api.delete(id);
    }

    @Override
    public List<SchemaVersion> getDataTypes() {
        return api.getDataTypes();
    }

}
