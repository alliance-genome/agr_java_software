package org.alliancegenome.agr_submission.interfaces.client;

import java.util.List;

import org.alliancegenome.agr_submission.entities.DataSubType;
import org.alliancegenome.agr_submission.interfaces.server.DataSubTypeControllerInterface;

import si.mazi.rescu.RestProxyFactory;

public class DataSubTypeControllerClientAPI implements DataSubTypeControllerInterface {

    private DataSubTypeControllerInterface api;
    
    public DataSubTypeControllerClientAPI() { }
    
    public DataSubTypeControllerClientAPI(String baseUrl) {
        api = RestProxyFactory.createProxy(DataSubTypeControllerInterface.class, baseUrl);
    }
    
    @Override
    public DataSubType create(DataSubType entity) {
        return api.create(entity);
    }

    @Override
    public DataSubType get(Long id) {
        return api.get(id);
    }

    @Override
    public DataSubType update(DataSubType entity) {
        return api.update(entity);
    }

    @Override
    public DataSubType delete(Long id) {
        return api.delete(id);
    }

    @Override
    public List<DataSubType> getDataTypes() {
        return api.getDataTypes();
    }

}
