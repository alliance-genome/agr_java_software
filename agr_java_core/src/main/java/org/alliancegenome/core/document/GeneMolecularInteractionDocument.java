package org.alliancegenome.core.document;

import org.alliancegenome.core.view.PublicView;
import org.alliancegenome.curation_api.model.document.es.ESDocument;
import org.alliancegenome.curation_api.model.entities.GeneMolecularInteraction;

import com.fasterxml.jackson.annotation.JsonView;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonView({PublicView.MolecularInteraction.class})
public class GeneMolecularInteractionDocument extends ESDocument {
	{
		category = "gene_molecular_interaction";
	}
	
	private GeneMolecularInteraction geneMolecularInteraction;
}
