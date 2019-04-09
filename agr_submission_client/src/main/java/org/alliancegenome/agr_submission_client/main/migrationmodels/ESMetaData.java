package org.alliancegenome.agr_submission_client.main.migrationmodels;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter @ToString
public class ESMetaData extends ESHit {
    private ESSourceMetaData _source;

    @Override
    public void generateAPICalls() {
        
        
    }
}
