package org.alliancegenome.agr_submission_client.main.migrationmodels;

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
    
    //private static DataFileControllerClientAPI dataFileApi = new DataFileControllerClientAPI("http://localhost:8080/api");
    
    private ESSourceDataFile _source;

    @Override
    public void generateAPICalls() {
        
        DataFile df = new DataFile();
        df.setS3Path(_source.getS3path());
        df.setUploadDate(_source.getUploadDate());
        
        log.info("Sending: " + df);
        
        SchemaVersion schemaVersion = getSchemaVersion(_source.getSchemaVersion());
        DataType dataType = getDataType(_source.getDataType());
        DataSubType subType = getDataSubType(_source.mapTaxonIDPartToModname());
        
        ESDataFile.dataFileApi.create(schemaVersion.getSchema(), dataType.getName(), subType.getName(), df);
    }
    

}
