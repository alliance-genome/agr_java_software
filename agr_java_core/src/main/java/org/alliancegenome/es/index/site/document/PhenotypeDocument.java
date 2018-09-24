package org.alliancegenome.es.index.site.document;


import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PhenotypeDocument extends SearchableItemDocument {

    public static final String CATEGORY = "termName";
    {
        category = CATEGORY;
    }

    private String primaryKey;
    private String phenotypeStatement;
    private List<PhenotypeAnnotationDocument> annotations;

    @JsonIgnore
    public String getDocumentId() {
        return primaryKey;
    }
}
