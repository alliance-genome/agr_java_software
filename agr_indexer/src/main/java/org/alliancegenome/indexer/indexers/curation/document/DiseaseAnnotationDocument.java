package org.alliancegenome.indexer.indexers.curation.document;

import groovy.transform.EqualsAndHashCode;
import lombok.Data;
import org.alliancegenome.curation_api.model.entities.DiseaseAnnotation;
import org.alliancegenome.curation_api.model.entities.Reference;
import org.alliancegenome.curation_api.model.entities.VocabularyTerm;
import org.alliancegenome.curation_api.model.entities.ontology.DOTerm;
import org.alliancegenome.curation_api.model.entities.ontology.ECOTerm;
import org.alliancegenome.es.index.site.document.SearchableItemDocument;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@EqualsAndHashCode(callSuper = true)
public class DiseaseAnnotationDocument extends SearchableItemDocument {

    private VocabularyTerm diseaseRelation;
    private DOTerm object;
    private List<ECOTerm> evidenceCodes;
    private String dataProvider;
    private Reference singleReference;
    private Set<Reference> references;

    private List<DiseaseAnnotation> primaryAnnotations;

    public DiseaseAnnotationDocument() {
        primaryAnnotations = new ArrayList<>();
    }

    public void addReference(Reference singleReference) {
        if (references == null) {
            references = new HashSet<>();
        }
        references.add(singleReference);
    }

    public void addPrimaryAnnotation(DiseaseAnnotation da) {
        if(primaryAnnotations == null){
            primaryAnnotations = new ArrayList<>();
        }
        primaryAnnotations.add(da);
    }
}
