package org.alliancegenome.indexer.document;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SourceDoclet {

    private SpeciesDoclet species;
    private String url;
    private String diseaseUrl;
    private String name;

}
