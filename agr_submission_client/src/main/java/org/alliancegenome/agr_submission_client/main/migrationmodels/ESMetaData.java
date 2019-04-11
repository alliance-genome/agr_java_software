package org.alliancegenome.agr_submission_client.main.migrationmodels;

import org.alliancegenome.agr_submission.entities.DataType;
import org.alliancegenome.agr_submission.entities.ReleaseVersion;
import org.alliancegenome.agr_submission.entities.SchemaVersion;
import org.alliancegenome.agr_submission.forms.CreateSchemaFileForm;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Getter @Setter @ToString
public class ESMetaData extends ESHit {
    
    private ESSourceMetaData _source;

    @Override
    public void generateAPICalls() {
        log.debug(_source.getDataTypes());
        
        for(String schema: _source.getSchemas()) {
            createSchema(schema);
        }
        
        for(String release: _source.getReleaseSchemaMap().keySet()) {
            ReleaseVersion rv = createRelease(release);
            SchemaVersion sv = createSchema(_source.getReleaseSchemaMap().get(release));
            addReleaseSchema(rv.getReleaseVersion(), sv.getSchema());
        }
        
        
        for(ESDataType dt: _source.getDataTypes().values()) {
            DataType dataType = getDataType(dt.getName());
            if(dataType == null) {
                dataType = new DataType();
                dataType.setName(dt.getName());
                dataType.setFileExtension(dt.getFileExtension());
                dataType.setDescription(dt.getDescription());
                dataType.setDataSubTypeRequired(dt.getTaxonIdRequired());
                dataType.setValidationRequired(dt.getValidationRequired());
                createDataType(dataType);
            } else {
                log.info("Data Type already exists: " + dataType);
            }
            
            CreateSchemaFileForm form = new CreateSchemaFileForm();
            for(String key: dt.getSchemaFiles().keySet()) {
                form.setFilePath(dt.getSchemaFiles().get(key));
                form.setSchema(key);
                createSchema(key);
                DataType newDataType = addSchemaFile(dataType.getName(), form);
                log.debug("New Data Type: " + newDataType);
            }
            
            
        }
    }
    
    private ReleaseVersion createRelease(String release) {
        ReleaseVersion rv = getReleaseVersion(release);
        if(rv == null) {
            rv = new ReleaseVersion();
            rv.setReleaseVersion(release);
            return createReleaseVersion(rv);
        } else {
            log.info("Release Version already exists: " + rv);
            return rv;
        }
    }

    private SchemaVersion createSchema(String schema) {
        SchemaVersion sv = getSchemaVersion(schema);
        if(sv == null) {
            sv = new SchemaVersion();
            sv.setSchema(schema);
            return createSchemaVersion(sv);
        } else {
            log.info("Schema Version already exists: " + sv);
            return sv;
        }
    }

}



//private String name;
//private String fileExtension;
//private String description;
//private Boolean taxonIdRequired;
//private Boolean validationRequired;
//private Boolean modVersionStored;
//private Map<String, String> schemaFiles;