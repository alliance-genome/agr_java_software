package org.alliancegenome.es.index.site.document;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import org.alliancegenome.es.index.ESDocument;
import org.alliancegenome.es.index.site.doclet.PublicationDoclet;
import org.alliancegenome.es.index.site.doclet.SourceDoclet;

import java.util.List;

@Getter @Setter
public class PhenotypeAnnotationDocument extends SearchableItemDocument implements Comparable<PhenotypeAnnotationDocument> {

    public static final String CATEGORY = "phenotypeAnnotation";
    {
        category = CATEGORY;
    }
    private String primaryKey;
    private SourceDoclet source;
    private String phenotype;
    private GeneDocument geneDocument;
    private FeatureDocument featureDocument;
    private List<PublicationDoclet> publications;
    private String geneticEntity;

    @JsonIgnore
    public String getDocumentId() {
        return primaryKey;
    }

    @Override
    public int compareTo(PhenotypeAnnotationDocument doc) {
        return 0;
    }

    public String getGeneticEntity() {
        return featureDocument != null ? featureDocument.category : geneDocument.category;
    }
}
