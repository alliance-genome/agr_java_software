package org.alliancegenome.es.index.site.document;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import org.alliancegenome.es.index.ESDocument;
import org.alliancegenome.es.index.site.doclet.PublicationDoclet;
import org.alliancegenome.es.index.site.doclet.SourceDoclet;

import java.util.List;

@Getter @Setter
public class PhenotypeAnnotationDocument extends ESDocument implements Comparable<PhenotypeAnnotationDocument> {

    public static final String CATEGORY = "phenotypeAnnotation";
    private String primaryKey;
    private String category = CATEGORY;
    private SourceDoclet source;
    private String phenotype;
    private GeneDocument geneDocument;
    private FeatureDocument featureDocument;
    private List<PublicationDoclet> publications;

    @JsonIgnore
    public String getDocumentId() {
        return primaryKey;
    }
    
    @Override
    @JsonIgnore
    public String getType() {
        return category;
    }

    @Override
    public int compareTo(PhenotypeAnnotationDocument doc) {
        return 0;
    }
}
