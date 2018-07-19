package org.alliancegenome.es.index.site.document;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class GoDocument extends SearchableItemDocument {

    public static final String CATEGORY = "go";
    {
        category = CATEGORY;
    }

    private String id;
    private String go_type;
    private String href;
    private List<String> synonyms;
    private List<String> go_genes;
    private List<String> go_species;

    @Override
    @JsonIgnore
    public String getDocumentId() {
        return id;
    }
    
}
