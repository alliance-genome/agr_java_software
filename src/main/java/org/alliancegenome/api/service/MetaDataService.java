package org.alliancegenome.api.service;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;

import javax.enterprise.context.RequestScoped;

import org.alliancegenome.core.exceptions.GenericException;
import org.alliancegenome.core.exceptions.SchemaDataTypeException;
import org.alliancegenome.core.exceptions.ValidataionException;
import org.alliancegenome.es.index.data.dao.MetaDataDAO;
import org.alliancegenome.es.index.data.doclet.DataTypeDoclet;
import org.alliancegenome.es.index.data.doclet.SnapShotDoclet;
import org.alliancegenome.es.index.site.doclet.SpeciesDoclet;
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

    public void submitData(String key, File inFile) throws GenericException {

        // Split the keys by underscore
        String[] keys = key.split("_");

        String schemaLookup;
        String dataTypeLookup;
        String speciesLookup;
        
        if(keys.length == 3) { // Schema-DataType-TaxonId
            log.debug("Key has 3 items: parse: (Schema-DataType-TaxonId): " + key);
            schemaLookup = keys[0];
            dataTypeLookup = keys[1];
            speciesLookup = keys[2];
        } else if(keys.length == 2) { // DataType-TaxonId // Input a taxonId datatype file and validate against latest version of schema
            log.debug("Key has 2 items: parse: (DataType-TaxonId): " + key);
            schemaLookup = null;
            dataTypeLookup = keys[0];
            speciesLookup = keys[1];
        } else {
            throw new ValidataionException("Wrong Number of Args for File Data: " + key);
        }
        
        String schemaVersion = getSchemaVersion(schemaLookup);
        DataTypeDoclet dataType = getDataType(dataTypeLookup);
        SpeciesDoclet species = getSpecies(dataType, speciesLookup);

        // This code can be removed if the loader takes over for GO, SO, DO ontologies
//      } else if(keys.length == 1) { // DataType // Input a datatype file validate against latest version of the schema (GO, SO, DO) maybe certain datatypes don't need validation
//          log.debug("Key has 1 items: parse: (DataType): " + key);
//
//          schemaVersion = getSchemaVersion(null);
//          dataType = getDataType(keys[0]);
//          
//          if(dataType.isTaxonIdRequired() || dataType.isValidationRequired()) {
//              throw new ValidataionException("Schema or TaxonId is required for this data type however no schema or taxonid was provided: " + dataType);
//          }
        
        if(dataType.isValidationRequired()) {
            validateData(schemaVersion, dataType, inFile);
        }
        
        if(dataType.isModVersionStored()) {

        }
        
        saveFile(schemaVersion, dataType, species, inFile);
    }

    public boolean validateData(String key, File inFile) throws GenericException {
        // Split the keys by underscore
        String[] keys = key.split("_");

        String schemaLookup;
        String dataTypeLookup;
        String speciesLookup;
        
        if(keys.length == 3) { // Schema-DataType-TaxonId
            log.debug("Key has 3 items: parse: (Schema-DataType-TaxonId): " + key);
            schemaLookup = keys[0];
            dataTypeLookup = keys[1];
            speciesLookup = keys[2];
        } else if(keys.length == 2) { // DataType-TaxonId // Input a taxonId datatype file and validate against latest version of schema
            log.debug("Key has 2 items: parse: (DataType-TaxonId): " + key);
            schemaLookup = null;
            dataTypeLookup = keys[0];
            speciesLookup = keys[1];
        } else {
            throw new ValidataionException("Wrong Number of Args for File Data: " + key);
        }
        
        String schemaVersion = getSchemaVersion(schemaLookup);
        DataTypeDoclet dataType = getDataType(dataTypeLookup);
        SpeciesDoclet species = getSpecies(dataType, speciesLookup);

        // This code can be removed if the loader takes over for GO, SO, DO ontologies
//      } else if(keys.length == 1) { // DataType // Input a datatype file validate against latest version of the schema (GO, SO, DO) maybe certain datatypes don't need validation
//          log.debug("Key has 1 items: parse: (DataType): " + key);
//
//          schemaVersion = getSchemaVersion(null);
//          dataType = getDataType(keys[0]);
//          
//          if(dataType.isTaxonIdRequired() || dataType.isValidationRequired()) {
//              throw new ValidataionException("Schema or TaxonId is required for this data type however no schema or taxonid was provided: " + dataType);
//          }
        
        if(dataType.isValidationRequired()) {
            return validateData(schemaVersion, dataType, inFile);
        }
        
        throw new ValidataionException("This file can not be validated: " + key);
    }

    private boolean validateData(String schemaVersionName, DataTypeDoclet dataType, File inFile) throws GenericException {
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
            JsonNode jsonNode = JsonLoader.fromFile(inFile);

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

    private String getSchemaVersion(String schemaString) throws ValidataionException {
        if(schemaString == null) {
            String schemaVersion = metaDataDAO.getCurrentSchemaVersion();
            return schemaVersion;
        } else {
            String schemaVersion = metaDataDAO.getSchemaVersion(schemaString);
            if(schemaVersion == null) {
                throw new ValidataionException("Schema Version not found: " + schemaString);
            }
            return schemaVersion;
        }
    }

    private void saveFile(String schemaVersion, DataTypeDoclet dataType, SpeciesDoclet species, File inFile) throws GenericException {
        if(species == null) {
            String filePath = metaDataDAO.saveFileToS3(schemaVersion, dataType, inFile);
            metaDataDAO.createDataFile(schemaVersion, dataType, null, filePath);
        } else {
            String filePath = metaDataDAO.saveFileToS3(schemaVersion, dataType, species, inFile);
            metaDataDAO.createDataFile(schemaVersion, dataType, species, filePath);
        }
    }

    private SpeciesDoclet getSpecies(DataTypeDoclet dataType, String speciesString) throws ValidataionException {
        if(dataType.isTaxonIdRequired()) {
            SpeciesDoclet species = metaDataDAO.getSpeciesDoclet(speciesString);
            if(species == null) {
                throw new ValidataionException("Species for taxonId not found: " + speciesString);
            }
            return species;
        } else {
            // TaxonId is not required
            throw new ValidataionException("TaxonId is not required for this data type: " + dataType);
        }
    }

    private DataTypeDoclet getDataType(String dataTypeString) throws ValidataionException {
        DataTypeDoclet dataType = metaDataDAO.getDataType(dataTypeString);
        if(dataType == null) {
            throw new ValidataionException("Data Type not found: " + dataTypeString);
        }
        return dataType;
    }

    public SnapShotDoclet getShapShot(String system, String releaseVersion) {
        return metaDataDAO.getSnapShot(system, releaseVersion);
    }

    public HashMap<String, Date> getReleases(String system) {
        return metaDataDAO.getReleases(system);
    }

    public SnapShotDoclet takeSnapShot(String system, String releaseVersion) {
        return metaDataDAO.takeSnapShot(system, releaseVersion);
    }


}