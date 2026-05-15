package org.alliancegenome.api.entity;

import org.alliancegenome.curation_api.model.document.es.ESDocument;
import org.alliancegenome.curation_api.model.entities.GeneGeneticInteraction;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class GeneGeneticInteractionDocument extends ESDocument {
	{
		category = "gene_genetic_interaction";
	}

	private GeneGeneticInteraction geneGeneticInteraction;
}
