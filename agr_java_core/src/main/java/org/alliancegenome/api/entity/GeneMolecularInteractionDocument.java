package org.alliancegenome.api.entity;

import org.alliancegenome.curation_api.model.entities.GeneMolecularInteraction;
import org.alliancegenome.es.index.ESDocument;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data @EqualsAndHashCode(callSuper = true)
public class GeneMolecularInteractionDocument extends ESDocument {
	
	String category = "gene_molecular_interaction";
	@Override
	public String getType() {
		return category;
	}
	
	GeneMolecularInteraction geneMolecularInteraction;
	
}
