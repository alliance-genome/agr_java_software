package org.alliancegenome.api.entity;

import org.alliancegenome.curation_api.model.document.es.ESDocument;
import org.alliancegenome.curation_api.model.entities.GeneGeneticInteraction;
import org.alliancegenome.neo4j.view.View;

import com.fasterxml.jackson.annotation.JsonView;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class GeneGeneticInteractionDocument extends ESDocument {
	{
		category = "gene_genetic_interaction";
	}
	@JsonView({View.GeneticInteraction.class})
	private GeneGeneticInteraction geneGeneticInteraction;
}
