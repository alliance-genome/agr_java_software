package org.alliancegenome.indexer.indexers.curation.document;

import lombok.EqualsAndHashCode;
import org.alliancegenome.curation_api.model.entities.Allele;

import lombok.Data;

@Data @EqualsAndHashCode(callSuper = true)
public class AlleleDiseaseAnnotationDocument extends DiseaseAnnotationDocument {
	
	private Allele subject;
	
	public AlleleDiseaseAnnotationDocument() {
		setCategory("allele_disease_annotation");
	}

}
