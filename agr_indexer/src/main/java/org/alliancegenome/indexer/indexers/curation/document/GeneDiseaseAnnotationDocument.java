package org.alliancegenome.indexer.indexers.curation.document;

import org.alliancegenome.curation_api.model.entities.DiseaseAnnotation;
import org.alliancegenome.curation_api.model.entities.Gene;

import groovy.transform.EqualsAndHashCode;
import lombok.Data;

import java.util.List;

@Data @EqualsAndHashCode(callSuper = true)
public class GeneDiseaseAnnotationDocument extends DiseaseAnnotationDocument {
	
	private Gene subject;

	public GeneDiseaseAnnotationDocument() {
		setCategory("gene_disease_annotation");
	}

}
