package org.alliancegenome.indexer.indexers.curation.document;

import org.alliancegenome.curation_api.model.entities.AffectedGenomicModel;

import groovy.transform.EqualsAndHashCode;
import lombok.Data;

@Data @EqualsAndHashCode(callSuper = true)
public class AGMDiseaseAnnotationDocument extends DiseaseAnnotationDocument {
	
	private AffectedGenomicModel subject;
	
	public AGMDiseaseAnnotationDocument() {
		setCategory("agm_disease_annotation");
	}
	
}
