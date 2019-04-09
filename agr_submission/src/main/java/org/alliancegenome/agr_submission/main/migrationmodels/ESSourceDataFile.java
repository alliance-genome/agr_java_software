package org.alliancegenome.agr_submission.main.migrationmodels;

import java.util.Date;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter @ToString
public class ESSourceDataFile {

    private String schemaVersion;
    private String dataType;
    private String taxonIDPart;
    private String s3path;
    private Date uploadDate;
    
    public String mapTaxonIDPartToModname() {
        if(taxonIDPart.equals("10116")) return "RGD";
        if(taxonIDPart.equals("6239")) return "WB";
        if(taxonIDPart.equals("4932")) return "SGD";
        if(taxonIDPart.equals("559292")) return "SGD";
        if(taxonIDPart.equals("7955")) return "ZFIN";
        if(taxonIDPart.equals("10090")) return "MGI";
        if(taxonIDPart.equals("7227")) return "FB";
        if(taxonIDPart.equals("9606")) return "HUMAN";
        return "";
    }
    
}
