package org.alliancegenome.agr_submission.interfaces.client;

import java.util.List;

import org.alliancegenome.agr_submission.entities.DataType;
import org.alliancegenome.agr_submission.forms.AddDataSubTypeForm;
import org.alliancegenome.agr_submission.forms.CreateSchemaFileForm;
import org.alliancegenome.agr_submission.interfaces.server.DataTypeControllerInterface;

import si.mazi.rescu.RestProxyFactory;

public class DataTypeControllerClientAPI implements DataTypeControllerInterface {

    private DataTypeControllerInterface api;
    
    public DataTypeControllerClientAPI() { }
    
    public DataTypeControllerClientAPI(String baseUrl) {
        api = RestProxyFactory.createProxy(DataTypeControllerInterface.class, baseUrl);
    }
    
    @Override
    public DataType create(DataType entity) {
        return api.create(entity);
    }

    @Override
    public DataType get(Long id) {
        return api.get(id);
    }

    @Override
    public DataType update(DataType entity) {
        return api.update(entity);
    }

    @Override
    public DataType delete(Long id) {
        return api.delete(id);
    }

    @Override
    public List<DataType> getDataTypes() {
        return api.getDataTypes();
    }

    @Override
    public DataType addSchemaFile(String dataType, CreateSchemaFileForm form) {
        return api.addSchemaFile(dataType, form);
    }

    @Override
    public DataType addDataSubType(String dataType, AddDataSubTypeForm form) {
        return api.addDataSubType(dataType, form);
    }

}
