package org.alliancegenome.agr_submission_client.main;

import org.alliancegenome.agr_submission.config.ConfigHelper;
import org.alliancegenome.agr_submission_client.main.migrationmodels.ESDataFile;
import org.alliancegenome.agr_submission_client.main.migrationmodels.ESHit;
import org.alliancegenome.agr_submission_client.main.migrationmodels.ESMetaData;
import org.alliancegenome.agr_submission_client.main.migrationmodels.ESResultInterface;
import org.alliancegenome.agr_submission_client.main.migrationmodels.ESResults;
import org.alliancegenome.agr_submission_client.main.migrationmodels.ESSnapshot;

import lombok.extern.log4j.Log4j2;
import si.mazi.rescu.RestProxyFactory;

@Log4j2
public class MainESMigration {

    // https://es.alliancegenome.org/data_index/_search?size=1000
    public static void main(String[] args) {
        ConfigHelper.init();
        
        ESResultInterface api = RestProxyFactory.createProxy(ESResultInterface.class, "https://es.alliancegenome.org");
        
        ESResults result = api.getResults("Basic YWRtaW46NGRtMW4=", 1000);
        //log.info("Does this happen?");
        for(ESHit hit: result.getHits().getHits()) {
            //log.info(hit);
            if(hit instanceof ESDataFile) {
                //log.info("DataFile: " + hit);
                //hit.generateAPICalls();
            } else if(hit instanceof ESSnapshot) {
                // Run Last
                //log.info("ESSnapshot: " + hit);
                //hit.generateAPICalls();
            } else if(hit instanceof ESMetaData) {
                // Run First
                //log.info("ESMetaData: " + hit);
                //hit.generateAPICalls();
            }
            //System.exit(-1);
        }
        //ESHit.addSubTypes(ESDataFile.typeToSubTypes);

    }
}
