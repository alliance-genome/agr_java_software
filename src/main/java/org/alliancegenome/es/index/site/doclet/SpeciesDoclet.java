package org.alliancegenome.es.index.site.doclet;

import org.alliancegenome.es.index.doclet.Doclet;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SpeciesDoclet extends Doclet {

    private String name;
    private String taxonID;
    private String displayName;
    private String abbreviation;
    private String modName;
    private String databaseName;
    private String taxonIDPart;
    
    private int orderID;
    
}
