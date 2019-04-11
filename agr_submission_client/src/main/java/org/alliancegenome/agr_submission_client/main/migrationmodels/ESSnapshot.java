package org.alliancegenome.agr_submission_client.main.migrationmodels;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Getter @Setter @ToString
public class ESSnapshot extends ESHit {
    private ESSourceSnapshot _source;

    @Override
    public void generateAPICalls() {
        
        log.debug(_source.getReleaseSnapShotMap());
        
        for(String release: _source.getReleaseSnapShotMap().keySet()) {
            createSnapshot(release, _source.getReleaseSnapShotMap().get(release));
        }
    }

}
