package org.alliancegenome.api.entity;

import com.fasterxml.jackson.annotation.JsonView;
import org.alliancegenome.curation_api.model.entities.Gene;

import lombok.Data;
import lombok.EqualsAndHashCode;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.alliancegenome.neo4j.view.View;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonPropertyOrder({ "subject", "gene", "relation", "object", "primaryAnnotations" })
@JsonView({View.DiseaseAnnotationAll.class})
public class GeneDiseaseAnnotationDocument extends DiseaseAnnotationDocument {

	private Gene subject;

	public GeneDiseaseAnnotationDocument() {
		setCategory("gene_disease_annotation");
	}

}
