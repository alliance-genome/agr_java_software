package org.alliancegenome.indexer.document;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class GoDocument extends SearchableItem {

    { category = "go"; }

    private String href;
    private String go_type;
    private List<String> synonyms;
    private List<String> go_genes;
    private List<String> go_species;

}
