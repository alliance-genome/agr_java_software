package org.alliancegenome.core.document;

import org.alliancegenome.curation_api.model.entities.Gene;
import org.alliancegenome.core.view.PublicView;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonView;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonPropertyOrder({"subject", "gene", "relation", "object", "primaryAnnotations"})
@JsonView({PublicView.DiseaseAnnotationAll.class})
public class GeneDiseaseAnnotationDocument extends DiseaseAnnotationDocument {

	private Gene subject;

	public GeneDiseaseAnnotationDocument() {
		setCategory("gene_disease_annotation");
	}

}
