package org.alliancegenome.api.entity;

import org.alliancegenome.curation_api.model.entities.Gene;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class GeneDiseaseAnnotationDocument extends DiseaseAnnotationDocument {

	private Gene subject;

	public GeneDiseaseAnnotationDocument() {
		setCategory("gene_disease_annotation");
	}

}
