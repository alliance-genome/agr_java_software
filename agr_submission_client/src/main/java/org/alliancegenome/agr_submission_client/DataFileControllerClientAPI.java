package org.alliancegenome.agr_submission_client;

import java.util.List;

import org.alliancegenome.agr_submission.entities.DataFile;
import org.alliancegenome.agr_submission.interfaces.server.DataFileControllerInterface;

import si.mazi.rescu.RestProxyFactory;

public class DataFileControllerClientAPI implements DataFileControllerInterface {

    private DataFileControllerInterface api;
    
    public DataFileControllerClientAPI() { }
    
    public DataFileControllerClientAPI(String baseUrl) {
        api = RestProxyFactory.createProxy(DataFileControllerInterface.class, baseUrl);
    }
    
    @Override
    public DataFile create(String schemaVersion, String dataType, String dataSubtype, DataFile entity) {
        return api.create(schemaVersion, dataType, dataSubtype, entity);
    }

    @Override
    public DataFile get(Long id) {
        return api.get(id);
    }

    @Override
    public DataFile update(DataFile entity) {
        return api.update(entity);
    }

    @Override
    public DataFile delete(Long id) {
        return api.delete(id);
    }

    @Override
    public List<DataFile> getDataFiles() {
        return api.getDataFiles();
    }

    @Override
    public List<DataFile> getDataTypeFiles(String dataType) {
        return api.getDataTypeFiles(dataType);
    }

    @Override
    public List<DataFile> getDataTypeSubTypeFiles(String dataType, String dataSubtype) {
        return api.getDataTypeSubTypeFiles(dataType, dataSubtype);
    }

}
