package org.alliancegenome.api.service;

import java.io.File;
import java.io.IOException;

import javax.enterprise.context.RequestScoped;

import org.alliancegenome.core.exceptions.GenericException;
import org.alliancegenome.core.exceptions.SchemaDataTypeException;
import org.alliancegenome.core.exceptions.ValidataionException;
import org.alliancegenome.es.index.data.dao.MetaDataDAO;
import org.alliancegenome.es.index.data.document.DataTypeDoclet;
import org.alliancegenome.es.index.data.document.TaxonIdDoclet;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingMessage;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;

@RequestScoped
public class MetaDataService {


    private static MetaDataDAO metaDataDAO = new MetaDataDAO();

    private Logger log = Logger.getLogger(getClass());

    public void submitData(String key, String bodyString) throws GenericException {

        String[] keys = key.split("-");
        
        // Get MetaData object

        if(keys.length == 3) { // Schema-DataType-TaxonId
            log.debug("Key has 3 items: parse: (Schema-DataType-TaxonId): " + key);
            parseSchemaDataTypeTaxonId(keys, bodyString);
        } else if(keys.length == 2) { // DataType-TaxonId // Input a taxonId datatype file and validate against latest version of schema
            log.debug("Key has 2 items: parse: (DataType-TaxonId): " + key);
            parseDataTypeTaxonId(keys, bodyString);
        } else if(keys.length == 1) { // DataType // Input a datatype file validate against latest version of the schema (GO, SO, DO) maybe certain datatypes don't need validation
            log.debug("Key has 1 items: parse: (DataType): " + key);
            parseDataType(keys, bodyString);
        }
    }

    public boolean validateData(String key, String bodyAsString) {
        log.info("Run by the validation endpoint");
        log.info("Need to validate file: " + key);
        return false;
    }

    private boolean validateData(String schemaVersionName, DataTypeDoclet dataType, String bodyString) throws GenericException {
        log.info("Need to validate file: " + schemaVersionName + " " + dataType);
        String dataTypeFilePath = dataType.getSchemaFiles().get(schemaVersionName);

        if(dataTypeFilePath == null) {
            log.info("No Data type file found for: " + schemaVersionName + " looking backwards for older schema versions");

            String previousVersion = null;
            for(previousVersion = metaDataDAO.getPreviousVersion(schemaVersionName); previousVersion != null;  previousVersion = metaDataDAO.getPreviousVersion(previousVersion) ) {
                if(dataType.getSchemaFiles().get(previousVersion) != null) {
                    dataTypeFilePath = dataType.getSchemaFiles().get(previousVersion);
                    log.info("Found File name for: " + previousVersion + " -> " + dataTypeFilePath);
                    break;
                }
            }
            if(previousVersion == null) {
                throw new SchemaDataTypeException("No Schema file for Data Type found: schema: " + schemaVersionName + " dataType: " + dataType.getName());
            } else {
                log.info("Previous Version Found: " + previousVersion);
            }
        }
        File schemaFile = metaDataDAO.getSchemaFile(schemaVersionName, dataTypeFilePath);

        try {

            JsonSchema schemaNode = JsonSchemaFactory.byDefault().getJsonSchema(schemaFile.toURI().toString());
            JsonNode jsonNode = JsonLoader.fromString(bodyString);

            ProcessingReport report = schemaNode.validate(jsonNode);

            if(!report.isSuccess()) {
                for(ProcessingMessage message: report) {
                    throw new ValidataionException(message.getMessage());
                }
            }
            log.info("Validation Complete: " + report.isSuccess());
            return report.isSuccess();
        } catch (IOException | ProcessingException e) {
            throw new ValidataionException(e.getMessage());
        }

    }

    private void parseDataType(String[] keys, String bodyString) throws GenericException {


        String schemaVersion = metaDataDAO.getLatestSchemaVersion();

        DataTypeDoclet dataType = metaDataDAO.getDataType(keys[0]);
        if(dataType == null) {
            throw new ValidataionException("Data Type not found: " + keys[0]);
        }

        if(dataType.isTaxonIdRequired() || dataType.isValidationRequired()) {
            throw new ValidataionException("Schema or TaxonId is required for this data type however no schema or taxonid was provided: " + dataType);
        }

        String filePath = metaDataDAO.saveFileToS3(schemaVersion, dataType, bodyString);
        
        metaDataDAO.createDataFile(schemaVersion, dataType, null, filePath);
    }

    private void parseDataTypeTaxonId(String[] keys, String bodyString) throws GenericException {
        TaxonIdDoclet taxonId;

        String schemaVersion = metaDataDAO.getLatestSchemaVersion();

        DataTypeDoclet dataType = metaDataDAO.getDataType(keys[0]);
        if(dataType == null) {
            throw new ValidataionException("Data Type not found: " + keys[0]);
        }

        if(dataType.isTaxonIdRequired()) {
            taxonId = metaDataDAO.getTaxonIdDocument(keys[1]);
            if(taxonId == null) {
                throw new ValidataionException("TaxonId not found: " + keys[1]);
            }
        } else {
            throw new ValidataionException("TaxonId is not required for this data type: " + dataType);
        }

        if(dataType.isValidationRequired()) {
            validateData(schemaVersion, dataType, bodyString);
        }

        String filePath = metaDataDAO.saveFileToS3(schemaVersion, dataType, taxonId, bodyString);
        
        metaDataDAO.createDataFile(schemaVersion, dataType, taxonId, filePath);
    }

    private void parseSchemaDataTypeTaxonId(String[] keys, String bodyString) throws GenericException {
        String schemaVersion;
        DataTypeDoclet dataType;
        TaxonIdDoclet taxonId;

        schemaVersion = metaDataDAO.getSchemaVersion(keys[0]);
        
        if(schemaVersion == null) {
            throw new ValidataionException("Schema Version not found: " + keys[0]);
        }

        dataType = metaDataDAO.getDataType(keys[1]);
        if(dataType == null) {
            throw new ValidataionException("Data Type not found: " + keys[1]);
        }

        if(dataType.isTaxonIdRequired()) {
            taxonId = metaDataDAO.getTaxonIdDocument(keys[2]);
            if(taxonId == null) {
                throw new ValidataionException("TaxonId not found: " + keys[2]);
            }
        } else {
            // TaxonId is not required
            throw new ValidataionException("TaxonId is not required for this data type: " + dataType);
        }

        if(dataType.isValidationRequired()) {
            validateData(schemaVersion, dataType, bodyString);
        }

        String filePath = metaDataDAO.saveFileToS3(schemaVersion, dataType, taxonId, bodyString);

        // Save File Document
        
        metaDataDAO.createDataFile(schemaVersion, dataType, taxonId, filePath);
    }


}