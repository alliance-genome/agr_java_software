package org.alliancegenome.agr_submission_client.main.migrationmodels;

import org.alliancegenome.agr_submission.entities.DataSubType;
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
        
        for(String key: _source.getSpecies().keySet()) {
            ESSpecies sp = _source.getSpecies().get(key);
            
            DataSubType subType = getDataSubType(sp.getDisplayName());

            if(subType == null) {
                subType = new DataSubType();
                subType.setDescription(sp.getDatabaseName());
                subType.setName(sp.getDisplayName());
                createDataSubType(subType);
            } else {
                log.info("Data Sub Type already exists: " + subType);
            }
            
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

