package org.alliancegenome.api.service;

import java.io.File;
import java.io.IOException;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import org.alliancegenome.api.dao.data.DataFileDAO;
import org.alliancegenome.api.dao.data.DataTypeDAO;
import org.alliancegenome.api.dao.data.SchemaDAO;
import org.alliancegenome.api.dao.data.TaxonIdDAO;
import org.alliancegenome.api.exceptions.GenericException;
import org.alliancegenome.api.exceptions.SchemaDataTypeException;
import org.alliancegenome.api.exceptions.ValidataionException;
import org.alliancegenome.api.model.esdata.DataFileDocument;
import org.alliancegenome.api.model.esdata.DataTypeDocument;
import org.alliancegenome.api.model.esdata.TaxonIdDocument;
import org.alliancegenome.api.model.esdata.SchemaDocument;
import org.alliancegenome.api.service.helper.git.GitHelper;
import org.alliancegenome.api.service.helper.git.S3Helper;
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

    //@Inject
    //private MetaDataDAO metaDataDAO;

    @Inject
    private GitHelper gitHelper;

    @Inject
    private TaxonIdDAO taxonIdDAO;

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

    private String saveFileToS3(SchemaDocument schemaVersion, DataTypeDocument dataType, String bodyString) throws GenericException {
        int fileIndex = s3Helper.listFiles(schemaVersion.getName() + "/" + dataType.getName() + "/");
        String filePath = schemaVersion.getName() + "/" + dataType.getName() + "/" + schemaVersion.getName() + "_" + dataType.getName() + "_" + fileIndex + "." + dataType.getFileExtension();
        s3Helper.saveFile(filePath, bodyString);
        return filePath;
    }

    private String saveFileToS3(SchemaDocument schemaVersion, DataTypeDocument dataType, TaxonIdDocument taxonId, String bodyString) throws GenericException {
        int fileIndex = s3Helper.listFiles(schemaVersion.getName() + "/" + dataType.getName() + "/" + taxonId.getName() + "/");

        String filePath =
                schemaVersion.getName() + "/" + dataType.getName() + "/" + taxonId.getName() + "/" +
                schemaVersion.getName() + "_" + dataType.getName() + "_" + taxonId.getName() + "_" + fileIndex + "." + dataType.getFileExtension();

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
            log.info("Validation Complete: " + report.isSuccess());
            return report.isSuccess();
        } catch (IOException | ProcessingException e) {
            throw new ValidataionException(e.getMessage());
        }

    }

    private void parseDataType(String[] keys, String bodyString) throws GenericException {
        SchemaDocument schemaVersion;
        DataTypeDocument dataType;

        schemaVersion = schemaDAO.getLatestSchemaVersion();

        dataType = dataTypeDAO.getDataType(keys[0]);
        if(dataType == null) {
            throw new ValidataionException("Data Type not found: " + keys[0]);
        }

        if(dataType.isTaxonIdRequired() || dataType.isValidationRequired()) {
            throw new ValidataionException("Schema or TaxonId is required for this data type however no schema or taxonid was provided: " + dataType);
        }

        String filePath = saveFileToS3(schemaVersion, dataType, bodyString);

        DataFileDocument dfd = new DataFileDocument();
        dfd.setSchemaVersion(schemaVersion.getName());
        dfd.setDataType(dataType.getName());
        dfd.setPath(filePath);
        //dfd.setTaxonId(taxonId.getName());
        dataFileDAO.createDocumnet(dfd);

    }

    private void parseDataTypeTaxonId(String[] keys, String bodyString) throws GenericException {
        SchemaDocument schemaVersion;
        DataTypeDocument dataType;
        TaxonIdDocument taxonId;

        schemaVersion = schemaDAO.getLatestSchemaVersion();

        dataType = dataTypeDAO.getDataType(keys[0]);
        if(dataType == null) {
            throw new ValidataionException("Data Type not found: " + keys[0]);
        }

        if(dataType.isTaxonIdRequired()) {
            taxonId = taxonIdDAO.getTaxonIdDocument(keys[1]);
            if(taxonId == null) {
                throw new ValidataionException("TaxonId not found: " + keys[1]);
            }
        } else {
            throw new ValidataionException("TaxonId is not required for this data type: " + dataType);
        }

        if(dataType.isValidationRequired()) {
            validateData(schemaVersion, dataType, bodyString);
        }

        String filePath = saveFileToS3(schemaVersion, dataType, taxonId, bodyString);

        DataFileDocument dfd = new DataFileDocument();
        dfd.setSchemaVersion(schemaVersion.getName());
        dfd.setDataType(dataType.getName());
        dfd.setPath(filePath);
        dfd.setTaxonId(taxonId.getName());
        dataFileDAO.createDocumnet(dfd);

    }

    private void parseSchemaDataTypeTaxonId(String[] keys, String bodyString) throws GenericException {
        SchemaDocument schemaVersion;
        DataTypeDocument dataType;
        TaxonIdDocument taxonId;

        schemaVersion = schemaDAO.getSchemaVersion(keys[0]);

        if(schemaVersion == null) {
            throw new ValidataionException("Schema Version not found: " + keys[0]);
        }

        dataType = dataTypeDAO.getDataType(keys[1]);
        if(dataType == null) {
            throw new ValidataionException("Data Type not found: " + keys[1]);
        }

        if(dataType.isTaxonIdRequired()) {
            taxonId = taxonIdDAO.getTaxonIdDocument(keys[2]);
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

        String filePath = saveFileToS3(schemaVersion, dataType, taxonId, bodyString);

        // Save File Document
        DataFileDocument dfd = new DataFileDocument();
        dfd.setSchemaVersion(schemaVersion.getName());
        dfd.setDataType(dataType.getName());
        dfd.setPath(filePath);
        dfd.setTaxonId(taxonId.getName());
        dataFileDAO.createDocumnet(dfd);

    }



}