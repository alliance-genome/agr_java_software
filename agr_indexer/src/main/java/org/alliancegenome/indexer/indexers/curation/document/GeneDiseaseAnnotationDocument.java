package org.alliancegenome.indexer.indexers.curation.document;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.alliancegenome.curation_api.model.entities.Gene;

@Data
@EqualsAndHashCode(callSuper = true)
public class GeneDiseaseAnnotationDocument extends DiseaseAnnotationDocument {

	private Gene subject;

	public GeneDiseaseAnnotationDocument() {
		setCategory("gene_disease_annotation");
	}

}
