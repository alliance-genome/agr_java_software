package org.alliancegenome.core.document;

import org.alliancegenome.core.view.PublicView;
import org.alliancegenome.curation_api.model.document.es.ESDocument;
import org.alliancegenome.curation_api.model.entities.GeneGeneticInteraction;

import com.fasterxml.jackson.annotation.JsonView;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonView({PublicView.GeneticInteraction.class})
public class GeneGeneticInteractionDocument extends ESDocument {
	{
		category = "gene_genetic_interaction";
	}

	private GeneGeneticInteraction geneGeneticInteraction;
}
