package org.alliancegenome.api.entity;

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
