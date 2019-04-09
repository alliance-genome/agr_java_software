package org.alliancegenome.agr_submission.main.migrationmodels;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter @ToString
public class ESSpecies {
    
    private String name;
    private String taxonID;
    private String displayName;
    private String abbreviation;
    private String modName;
    private String databaseName;
    private String taxonIDPart;
    private Integer orderID;

}
