package org.alliancegenome.agr_submission.main.migrationmodels;

import org.alliancegenome.agr_submission.entities.DataFile;
import org.alliancegenome.agr_submission.interfaces.client.DataFileControllerClientAPI;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Getter @Setter @ToString
public class ESDataFile extends ESHit {

    private static DataFileControllerClientAPI dataFileApi = new DataFileControllerClientAPI("http://localhost:8080/api");
    private ESSourceDataFile _source;

    @Override
    public void generateAPICalls() {
        
        DataFile df = new DataFile();
        df.setS3Path(_source.getS3path());
        df.setUploadDate(_source.getUploadDate());
        log.info("Sending: " + df);
        ESDataFile.dataFileApi.create(_source.getSchemaVersion(), _source.getDataType(), _source.mapTaxonIDPartToModname(), df);
        
    }
}
