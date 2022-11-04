package org.alliancegenome.indexer.indexers.curation.document;

import java.util.ArrayList;
import java.util.List;

import org.alliancegenome.curation_api.model.entities.DiseaseAnnotation;
import org.alliancegenome.curation_api.model.entities.Reference;
import org.alliancegenome.curation_api.model.entities.VocabularyTerm;
import org.alliancegenome.curation_api.model.entities.ontology.DOTerm;
import org.alliancegenome.curation_api.model.entities.ontology.ECOTerm;
import org.alliancegenome.es.index.site.document.SearchableItemDocument;

import groovy.transform.EqualsAndHashCode;
import lombok.Data;

@Data @EqualsAndHashCode(callSuper = true)
public class DiseaseAnnotationDocument extends SearchableItemDocument {

	private VocabularyTerm diseaseRelation;
	private DOTerm object;
	private List<ECOTerm> evidenceCodes;
	private String dataProvider;
	private Reference singleReference;
	
	private List<DiseaseAnnotation> primaryAnnotations;
	
	public DiseaseAnnotationDocument() {
		primaryAnnotations = new ArrayList<>();
	}
}
