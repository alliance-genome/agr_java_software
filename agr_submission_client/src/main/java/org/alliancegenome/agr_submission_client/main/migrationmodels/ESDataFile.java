package org.alliancegenome.agr_submission_client.main.migrationmodels;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alliancegenome.agr_submission.entities.DataFile;
import org.alliancegenome.agr_submission.entities.DataSubType;
import org.alliancegenome.agr_submission.entities.DataType;
import org.alliancegenome.agr_submission.entities.SchemaVersion;
import org.alliancegenome.agr_submission_client.DataFileControllerClientAPI;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Getter @Setter @ToString
public class ESDataFile extends ESHit {

    private static DataFileControllerClientAPI dataFileApi = new DataFileControllerClientAPI("http://localhost:8080/api");

    private ESSourceDataFile _source;
    
    public static Map<String, List<String>> typeToSubTypes = new HashMap<>();

    @Override
    public void generateAPICalls() {
        
        DataFile df = new DataFile();
        df.setS3Path(_source.getS3path());
        df.setUploadDate(_source.getUploadDate());
        
        log.info("Sending: " + df);
        
        SchemaVersion schemaVersion = getSchemaVersion(_source.getSchemaVersion());
        DataType dataType = getDataType(_source.getDataType());
        DataSubType subType = getDataSubType(_source.mapTaxonIDPartToModname());
        
        if(!ESDataFile.typeToSubTypes.containsKey(dataType.getName())) {
            ESDataFile.typeToSubTypes.put(dataType.getName(), new ArrayList<>());
        }
        if(!ESDataFile.typeToSubTypes.get(dataType.getName()).contains(subType.getName())) {
            ESDataFile.typeToSubTypes.get(dataType.getName()).add(subType.getName());
        }
        
        if(schemaVersion != null && dataType != null && subType != null) {
            ESDataFile.dataFileApi.create(schemaVersion.getSchema(), dataType.getName(), subType.getName(), df);
        } else {
            log.error("Data File info not found: " + _source.getSchemaVersion() + ": " + schemaVersion + " " + _source.getDataType() + ": " + dataType + " " + _source.mapTaxonIDPartToModname() + ": " + subType);
        }
        
    }

}
