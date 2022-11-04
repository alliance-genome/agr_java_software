package org.alliancegenome.indexer.indexers.curation.document;

import org.alliancegenome.curation_api.model.entities.Allele;

import groovy.transform.EqualsAndHashCode;
import lombok.Data;

@Data @EqualsAndHashCode(callSuper = true)
public class AlleleDiseaseAnnotationDocument extends DiseaseAnnotationDocument {
	
	private Allele subject;
	
	public AlleleDiseaseAnnotationDocument() {
		setCategory("allele_disease_annotation");
	}

}
