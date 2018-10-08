package org.alliancegenome.es.index.site.document;

import java.util.List;

import org.alliancegenome.es.index.ESDocument;
import org.alliancegenome.es.index.site.doclet.PublicationDoclet;
import org.alliancegenome.es.index.site.doclet.SourceDoclet;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;
import org.alliancegenome.es.index.site.doclet.SpeciesDoclet;
import org.alliancegenome.neo4j.entity.node.Species;

@Getter @Setter
public class AnnotationDocument extends ESDocument implements Comparable<AnnotationDocument> {
    
    private String primaryKey;
    private String category = "diseaseAnnotation";
    private String associationType;
    private SourceDoclet source;
    private SpeciesDoclet orthologySpecies;
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
    public int compareTo(AnnotationDocument doc) {
        return 0;
    }
}
