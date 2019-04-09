package org.alliancegenome.agr_submission.main.migrationmodels;

import java.util.Map;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter @ToString
public class ESDataType {

    private String name;
    private String fileExtension;
    private String description;
    private Boolean taxonIdRequired;
    private Boolean validationRequired;
    private Boolean modVersionStored;
    private Map<String, String> schemaFiles;
    
}
