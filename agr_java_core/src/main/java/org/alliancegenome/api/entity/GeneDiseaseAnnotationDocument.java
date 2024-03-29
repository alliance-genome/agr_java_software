package org.alliancegenome.api.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.alliancegenome.curation_api.model.entities.Gene;
import org.alliancegenome.neo4j.view.View;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonPropertyOrder({"subject", "gene", "relation", "object", "primaryAnnotations"})
@JsonView({View.DiseaseAnnotationAll.class})
public class GeneDiseaseAnnotationDocument extends DiseaseAnnotationDocument {

	private Gene subject;

	public GeneDiseaseAnnotationDocument() {
		setCategory("gene_disease_annotation");
	}

}
