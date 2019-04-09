package org.alliancegenome.agr_submission.main.migrationmodels;

import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter @ToString
public class ESSourceMetaData {

    private List<String> schemas;
    private Map<String, String> releaseSchemaMap;
    private Map<String, ESDataType> dataTypes;
    private Map<String, ESSpecies> species;
    
}
