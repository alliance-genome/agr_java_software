package org.alliancegenome.indexer.indexers.curation.document;

import org.alliancegenome.api.entity.DiseaseAnnotationDocument;
import org.alliancegenome.curation_api.model.entities.Allele;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data @EqualsAndHashCode(callSuper = true)
public class AlleleDiseaseAnnotationDocument extends DiseaseAnnotationDocument {
	
	private Allele subject;
	
	public AlleleDiseaseAnnotationDocument() {
		setCategory("allele_disease_annotation");
	}

}
