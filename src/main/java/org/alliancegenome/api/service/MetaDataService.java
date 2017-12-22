package org.alliancegenome.api.service;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import org.alliancegenome.api.dao.data.DataFileDAO;
import org.alliancegenome.api.dao.data.DataTypeDAO;
import org.alliancegenome.api.dao.data.ModDAO;
import org.alliancegenome.api.dao.data.SchemaDAO;
import org.alliancegenome.api.model.esdata.DataFileDocument;
import org.alliancegenome.api.model.esdata.DataTypeDocument;
import org.alliancegenome.api.model.esdata.ModDocument;
import org.alliancegenome.api.model.esdata.SchemaDocument;
import org.alliancegenome.api.rest.external.github.GithubRESTAPI;
import org.alliancegenome.api.rest.external.github.GithubRelease;
import org.jboss.logging.Logger;

@RequestScoped
public class MetaDataService {

    //@Inject
    //private MetaDataDAO metaDataDAO;

    @Inject
    private ModDAO modDAO;

    @Inject
    private DataTypeDAO dataTypeDAO;

    @Inject
    private SchemaDAO schemaDAO;

    @Inject
    private DataFileDAO dataFileDAO;

    private GithubRESTAPI githubAPI = new GithubRESTAPI();

    private Logger log = Logger.getLogger(getClass());

    public void submitData(String key, String bodyString) {

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
        log.debug("Parse failed due to no Params");
    }

    private String saveFileToS3(SchemaDocument schemaVersion, DataTypeDocument dataType, String bodyString) {
        // TODO save file to S3 and get the new file path
        log.info("Need to save file to S3");
        return null;
    }

    public boolean validateData(String key, String bodyAsString) {
        log.info("Run by the validation endpoint");
        log.info("Need to validate file: " + key);
        return false;
    }

    private boolean validateData(SchemaDocument schemaVersion, DataTypeDocument dataType, String bodyString) {
        // TODO validate
        log.info("Need to validate file: " + schemaVersion + " " + dataType);
        return false;
    }

    private void parseDataType(String[] keys, String bodyString) {
        SchemaDocument schemaVersion;
        DataTypeDocument dataType;

        schemaVersion = getLatestSchemaVersion();

        dataType = dataTypeDAO.readDocument(keys[0]);
        if(dataType == null) {
            log.debug("Data Type not found");
            return;
        }

        if(dataType.isModRequired() || dataType.isValidationRequired()) {
            log.debug("Schema or Mod is required for this data type however no schema or mod version was provided");
            return;
        }

        String filePath = saveFileToS3(schemaVersion, dataType, bodyString);
        if(filePath == null) {
            log.debug("File saving to S3 failed");
            return;
        }

        DataFileDocument dfd = new DataFileDocument();
        dfd.setSchemaVersion(schemaVersion.getName());
        dfd.setDataType(dataType.getName());
        dfd.setPath(filePath);
        //dfd.setMod(mod.getName());
        dataFileDAO.createDocumnet(dfd);

    }


    private void parseDataTypeMod(String[] keys, String bodyString) {
        SchemaDocument schemaVersion;
        DataTypeDocument dataType;
        ModDocument mod;

        schemaVersion = getLatestSchemaVersion();

        dataType = dataTypeDAO.readDocument(keys[0]);
        if(dataType == null) {
            log.debug("Data Type not found");
            return;
        }

        if(dataType.isModRequired()) {
            mod = modDAO.readDocument(keys[1]);
            if(mod == null) {
                log.debug("Mod not found");
                return;
            }
        } else {
            log.debug("Mod is not required for this data type");
            return;
        }

        if(dataType.isValidationRequired()) {
            if(!validateData(schemaVersion, dataType, bodyString)) {
                log.debug("File does not pass validation");
                return;
            }
        }

        // Save File
        String filePath = saveFileToS3(schemaVersion, dataType, bodyString);
        if(filePath == null) {
            log.debug("File saving to S3 failed");
            return;
        }

        DataFileDocument dfd = new DataFileDocument();
        dfd.setSchemaVersion(schemaVersion.getName());
        dfd.setDataType(dataType.getName());
        dfd.setPath(filePath);
        dfd.setMod(mod.getName());
        dataFileDAO.createDocumnet(dfd);

    }

    private void parseSchemaDataTypeMod(String[] keys, String bodyString) {
        SchemaDocument schemaVersion;
        DataTypeDocument dataType;
        ModDocument mod;

        schemaVersion = getSchemaVersion(keys[0]);
        if(schemaVersion == null) {
            log.debug("Schema Version not found");
            return;
        }

        dataType = dataTypeDAO.readDocument(keys[1]);
        if(dataType == null) {
            log.debug("Data Type not found");
            return;
        }

        if(dataType.isModRequired()) {
            mod = modDAO.readDocument(keys[2]);
            if(mod == null) {
                log.debug("Mod not found");
                return;
            }
        } else {
            // Mod is not required
            log.debug("Mod is not required for this data type");
            return;
        }

        if(dataType.isValidationRequired()) {
            if(!validateData(schemaVersion, dataType, bodyString)) {
                log.debug("File does not pass validation");
                return;
            }
        }

        // Save File
        String filePath = saveFileToS3(schemaVersion, dataType, bodyString);
        if(filePath == null) {
            log.debug("File saving to S3 failed");
            return;
        }

        // Save File Document
        DataFileDocument dfd = new DataFileDocument();
        dfd.setSchemaVersion(schemaVersion.getName());
        dfd.setDataType(dataType.getName());
        dfd.setPath(filePath);
        dfd.setMod(mod.getName());
        dataFileDAO.createDocumnet(dfd);

    }

    // Null -> Returns latest schema from github
    // Invalid schema -> null
    // Schema not in ES but in Github -> schema version
    private SchemaDocument getSchemaVersion(String string) {
        log.debug("Getting Schema Version");
        if(string != null) {
            SchemaDocument schemaVersion = schemaDAO.readDocument(string);
            log.debug("Schema Version: " + schemaVersion);
            if(schemaVersion == null) {
                GithubRelease gitHubSchema = githubAPI.getRelease("agr_schemas", string);

                if(gitHubSchema != null) {
                    schemaVersion = new SchemaDocument();
                    schemaVersion.setName(gitHubSchema.getName());
                    schemaDAO.createDocumnet(schemaVersion);
                    return schemaVersion;
                } else {
                    log.debug("Github Schema Version was null: " + gitHubSchema);
                    return null;
                }
            }
            return schemaVersion;
        } else {
            log.debug("Null Schema version");
            return getLatestSchemaVersion();
        }

    }

    private SchemaDocument getLatestSchemaVersion() {
        GithubRelease githubLatestRelease = githubAPI.getLatestRelease("agr_schemas");
        log.debug("Getting Latest Schema Version");
        SchemaDocument schemaVersion = schemaDAO.readDocument(githubLatestRelease.getName());
        log.debug("Schema Version: " + schemaVersion);
        if(schemaVersion == null) {
            schemaVersion = new SchemaDocument();
            schemaVersion.setName(githubLatestRelease.getName());
            schemaDAO.createDocumnet(schemaVersion);
        }
        return schemaVersion;
    }

}