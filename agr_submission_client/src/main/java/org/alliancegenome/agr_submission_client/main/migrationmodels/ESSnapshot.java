package org.alliancegenome.agr_submission_client.main.migrationmodels;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter @ToString
public class ESSnapshot extends ESHit {
    private ESSourceSnapshot _source;

    @Override
    public void generateAPICalls() {
        
        
    }
}
