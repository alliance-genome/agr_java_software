package org.alliancegenome.api.entity;

import org.alliancegenome.curation_api.model.entities.GeneGeneticInteraction;
import org.alliancegenome.es.index.ESDocument;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data @EqualsAndHashCode(callSuper = true)
public class GeneGeneticInteractionDocument extends ESDocument {
	
	String category = "gene_genetic_interaction";
	@Override
	public String getType() {
		return category;
	}
	
	GeneGeneticInteraction geneGeneticInteraction;
	
}
