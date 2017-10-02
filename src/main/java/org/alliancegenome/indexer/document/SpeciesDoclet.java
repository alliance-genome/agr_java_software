package org.alliancegenome.indexer.document;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SpeciesDoclet {

    private String name;
    private String taxonID;
    private String displayName;
    private int orderID;

}

