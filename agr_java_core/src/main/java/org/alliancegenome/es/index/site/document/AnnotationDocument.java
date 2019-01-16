package org.alliancegenome.es.index.site.document;

import java.util.List;

import org.alliancegenome.es.index.ESDocument;
import org.alliancegenome.es.index.site.doclet.PublicationDoclet;
import org.alliancegenome.es.index.site.doclet.SourceDoclet;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class AnnotationDocument extends ESDocument implements Comparable<AnnotationDocument> {
    
    private String primaryKey;
    private String category = "diseaseAnnotation";
    private String associationType;
    private SourceDoclet source;
    private GeneDocument orthologyGeneDocument;
    private GeneDocument geneDocument;
    private AlleleDocument alleleDocument;
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

    public AnnotationDocument() {
    }

    // for deep cloning purposes
    public AnnotationDocument(AnnotationDocument doc){
        primaryKey = doc.primaryKey;
        associationType = doc.associationType;
        // note: These attributes are not yet fully clones
        source = doc.source;
        orthologyGeneDocument = doc.orthologyGeneDocument;
        geneDocument = doc.geneDocument;
        alleleDocument = doc.alleleDocument;
        publications = doc.publications;
    }
}
