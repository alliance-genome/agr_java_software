package org.alliancegenome.api.entity;

import org.alliancegenome.curation_api.model.entities.Gene;
import org.alliancegenome.neo4j.view.View;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonView;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonPropertyOrder({"subject", "gene", "relation", "object", "primaryAnnotations"})
@JsonView({View.PhenotypeAnnotationAll.class})
public class GenePhenotypeAnnotationDocument extends PhenotypeAnnotationDocument {
	{
		category = "gene_phenotype_annotation";
	}
	private Gene subject;
}
