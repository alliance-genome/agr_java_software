package org.alliancegenome.api.entity;

import org.alliancegenome.curation_api.model.entities.AffectedGenomicModel;
import org.alliancegenome.neo4j.view.View;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonView;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonPropertyOrder({"subject", "gene", "relation", "object", "primaryAnnotations"})
@JsonView({View.DiseaseAnnotationAll.class})
public class AGMPhenotypeAnnotationDocument extends PhenotypeAnnotationDocument {
	{
		category = "agm_phenotype_annotation";
	}
	private AffectedGenomicModel subject;
}
