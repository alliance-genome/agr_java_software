package org.alliancegenome.api.entity;

import org.alliancegenome.curation_api.model.entities.GeneGeneticInteraction;
import org.alliancegenome.es.index.ESDocument;
import org.alliancegenome.neo4j.view.View;

import com.fasterxml.jackson.annotation.JsonView;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonView({View.GeneticInteraction.class})
public class GeneGeneticInteractionDocument extends ESDocument {
	
	String category = "gene_genetic_interaction";
	@Override
	public String getType() {
		return category;
	}
	
	GeneGeneticInteraction geneGeneticInteraction;
	
}
