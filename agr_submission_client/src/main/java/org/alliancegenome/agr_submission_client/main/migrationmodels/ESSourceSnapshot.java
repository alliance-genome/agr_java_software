package org.alliancegenome.agr_submission_client.main.migrationmodels;

import java.util.Map;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter @ToString
public class ESSourceSnapshot {

    private Map<String, Long> releaseSnapShotMap;
    
}

