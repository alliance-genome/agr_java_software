package org.alliancegenome.es.index.site.doclet;

import org.alliancegenome.es.index.ESDoclet;

import lombok.*;

@Getter
@Setter
public class SpeciesDoclet extends ESDoclet {

    private String name;
    private String taxonID;
    private String displayName;
    private String abbreviation;
    private String modName;
    private String databaseName;
    private String taxonIDPart;
    
    private int orderID;
    
}
