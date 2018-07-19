package org.alliancegenome.es.index.site.document;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import org.alliancegenome.es.index.site.doclet.CrossReferenceDoclet;
import org.alliancegenome.es.index.site.doclet.SourceDoclet;

import java.util.*;

@Getter
@Setter
public class PhenotypeDocument extends SearchableItemDocument {

    public static final String CATEGORY = "phenotype";
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
    
    @Override
    @JsonIgnore
    public String getType() {
        return category;
    }
}
