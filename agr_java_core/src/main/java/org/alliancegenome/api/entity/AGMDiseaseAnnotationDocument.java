package org.alliancegenome.indexer.indexers.curation.document;

import org.alliancegenome.api.entity.DiseaseAnnotationDocument;
import org.alliancegenome.curation_api.model.entities.AffectedGenomicModel;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data @EqualsAndHashCode(callSuper = true)
public class AGMDiseaseAnnotationDocument extends DiseaseAnnotationDocument {
	
	private AffectedGenomicModel subject;
	
	public AGMDiseaseAnnotationDocument() {
		setCategory("agm_disease_annotation");
	}
	
}
