package org.alliancegenome.api.entity;

import org.alliancegenome.curation_api.model.entities.Allele;
import org.alliancegenome.neo4j.view.PublicView;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonView;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonPropertyOrder({"subject", "gene", "relation", "object", "primaryAnnotations"})
@JsonView({PublicView.DiseaseAnnotationAll.class})
public class AllelePhenotypeAnnotationDocument extends PhenotypeAnnotationDocument {
	{
		category = "allele_phenotype_annotation";
	}
	private Allele subject;
}
