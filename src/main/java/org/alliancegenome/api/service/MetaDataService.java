package org.alliancegenome.api.service;

import java.io.File;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import org.alliancegenome.api.dao.data.DataFileDAO;
import org.alliancegenome.api.dao.data.DataTypeDAO;
import org.alliancegenome.api.dao.data.ModDAO;
import org.alliancegenome.api.dao.data.SchemaDAO;
import org.alliancegenome.api.exceptions.GenericException;
import org.alliancegenome.api.exceptions.SchemaDataTypeException;
import org.alliancegenome.api.exceptions.ValidataionException;
import org.alliancegenome.api.model.esdata.DataFileDocument;
import org.alliancegenome.api.model.esdata.DataTypeDocument;
import org.alliancegenome.api.model.esdata.ModDocument;
import org.alliancegenome.api.model.esdata.SchemaDocument;
import org.alliancegenome.api.service.helper.git.GitHelper;
import org.alliancegenome.api.service.helper.git.S3Helper;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.report.ProcessingMessage;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;

@RequestScoped
public class MetaDataService {

    //@Inject
    //private MetaDataDAO metaDataDAO;

    @Inject
    private GitHelper gitHelper;

    @Inject
    private ModDAO modDAO;

    @Inject
    private DataTypeDAO dataTypeDAO;

    @Inject
    private SchemaDAO schemaDAO;

    @Inject
    private DataFileDAO dataFileDAO;

    @Inject
    private S3Helper s3Helper;

    private Logger log = Logger.getLogger(getClass());

    public void submitData(String key, String bodyString) throws GenericException {

        String[] keys = key.split("-");

        if(keys.length == 3) { // Schema-DataType-Mod
            log.debug("Key has 3 items: parseSchemaDataTypeMod: " + key);
            parseSchemaDataTypeMod(keys, bodyString);
        } else if(keys.length == 2) { // DataType-Mod // Input a mod datatype file and validate against latest version of schema
            log.debug("Key has 2 items: parseDataTypeMod: " + key);
            parseDataTypeMod(keys, bodyString);
        } else if(keys.length == 1) { // DataType // Input a datatype file validate against latest version of the schema (GO, SO, DO) maybe certain datatypes don't need validation
            log.debug("Key has 1 items: parseDataType: " + key);
            parseDataType(keys, bodyString);
        }
    }

    private String saveFileToS3(DataTypeDocument dataType, String bodyString) throws GenericException {
        int fileIndex = s3Helper.listFiles(dataType.getName());
        String filePath = dataType.getName() + "/" + dataType.getName() + "_" + fileIndex + "." + dataType.getFileExtension();
        s3Helper.saveFile(filePath, bodyString);
        return filePath;
    }

    private String saveFileToS3(SchemaDocument schemaVersion, DataTypeDocument dataType, ModDocument mod, String bodyString) throws GenericException {
        int fileIndex = s3Helper.listFiles(schemaVersion.getName() + "/" + dataType.getName() + "/" + mod.getName());

        String filePath =
                schemaVersion.getName() + "/" + dataType.getName() + "/" + mod.getName() + "/" +
                schemaVersion.getName() + "_" + dataType.getName() + "_" + mod.getName() + "_" + fileIndex + "." + dataType.getFileExtension();

        s3Helper.saveFile(filePath, bodyString);
        return filePath;
    }

    public boolean validateData(String key, String bodyAsString) {
        log.info("Run by the validation endpoint");
        log.info("Need to validate file: " + key);
        return false;
    }

    private boolean validateData(SchemaDocument schemaVersion, DataTypeDocument dataType, String bodyString) throws GenericException {
        log.info("Need to validate file: " + schemaVersion + " " + dataType);

        String dataTypeFilePath = dataType.getSchemaFiles().get(schemaVersion.getName());
        if(dataTypeFilePath == null) {
            throw new SchemaDataTypeException("No Schema file for Data Type found: schema: " + schemaVersion.getName() + " dataType: " + dataType.getName());
        }
        File schemaFile = gitHelper.getFile(schemaVersion.getName(), dataTypeFilePath);

        try {

            JsonSchema schemaNode = JsonSchemaFactory.byDefault().getJsonSchema(schemaFile.toURI().toString());
            JsonNode jsonNode = JsonLoader.fromString(bodyString);

            ProcessingReport report = schemaNode.validate(jsonNode);

            if(!report.isSuccess()) {
                for(ProcessingMessage message: report) {
                    throw new ValidataionException(message.getMessage());
                }
            }
            return report.isSuccess();
        } catch (Exception e) {
            e.printStackTrace();
            throw new ValidataionException(e.getMessage());
        }

    }

    private void parseDataType(String[] keys, String bodyString) throws GenericException {
        DataTypeDocument dataType;

        dataType = dataTypeDAO.getDataType(keys[0]);
        if(dataType == null) {
            throw new ValidataionException("Data Type not found: " + keys[0]);
        }

        if(dataType.isModRequired() || dataType.isValidationRequired()) {
            throw new ValidataionException("Schema or Mod is required for this data type however no schema or mod version was provided");
        }

        String filePath = saveFileToS3(dataType, bodyString);

        DataFileDocument dfd = new DataFileDocument();
        dfd.setDataType(dataType.getName());
        dfd.setPath(filePath);
        //dfd.setMod(mod.getName());
        dataFileDAO.createDocumnet(dfd);

    }


    private void parseDataTypeMod(String[] keys, String bodyString) throws GenericException {
        SchemaDocument schemaVersion;
        DataTypeDocument dataType;
        ModDocument mod;

        schemaVersion = schemaDAO.getLatestSchemaVersion();

        dataType = dataTypeDAO.getDataType(keys[0]);
        if(dataType == null) {
            throw new ValidataionException("Data Type not found: " + keys[0]);
        }

        if(dataType.isModRequired()) {
            mod = modDAO.getModDocument(keys[1]);
            if(mod == null) {
                throw new ValidataionException("Mod not found: " + keys[1]);
            }
        } else {
            throw new ValidataionException("\"Mod is not required for this data type: " + dataType);
        }

        if(dataType.isValidationRequired()) {
            validateData(schemaVersion, dataType, bodyString);
        }

        String filePath = saveFileToS3(schemaVersion, dataType, mod, bodyString);

        DataFileDocument dfd = new DataFileDocument();
        dfd.setSchemaVersion(schemaVersion.getName());
        dfd.setDataType(dataType.getName());
        dfd.setPath(filePath);
        dfd.setMod(mod.getName());
        dataFileDAO.createDocumnet(dfd);

    }

    private void parseSchemaDataTypeMod(String[] keys, String bodyString) throws GenericException {
        SchemaDocument schemaVersion;
        DataTypeDocument dataType;
        ModDocument mod;

        schemaVersion = schemaDAO.getSchemaVersion(keys[0]);
        if(schemaVersion == null) {
            throw new ValidataionException("Schema Version not found: " + keys[0]);
        }

        dataType = dataTypeDAO.getDataType(keys[1]);
        if(dataType == null) {
            throw new ValidataionException("Data Type not found: " + keys[1]);
        }

        if(dataType.isModRequired()) {
            mod = modDAO.getModDocument(keys[2]);
            if(mod == null) {
                throw new ValidataionException("Mod not found: " + keys[2]);
            }
        } else {
            // Mod is not required
            throw new ValidataionException("Mod is not required for this data type: " + dataType);
        }

        if(dataType.isValidationRequired()) {
            validateData(schemaVersion, dataType, bodyString);
        }

        String filePath = saveFileToS3(schemaVersion, dataType, mod, bodyString);

        // Save File Document
        DataFileDocument dfd = new DataFileDocument();
        dfd.setSchemaVersion(schemaVersion.getName());
        dfd.setDataType(dataType.getName());
        dfd.setPath(filePath);
        dfd.setMod(mod.getName());
        dataFileDAO.createDocumnet(dfd);

    }



}