package org.alliancegenome.api.entity;

import org.alliancegenome.curation_api.model.document.es.ESDocument;
import org.alliancegenome.curation_api.model.entities.GeneMolecularInteraction;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class GeneMolecularInteractionDocument extends ESDocument {
	{
		category = "gene_molecular_interaction";
	}
	private GeneMolecularInteraction geneMolecularInteraction;
}
