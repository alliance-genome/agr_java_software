package org.alliancegenome.api.entity;

import org.alliancegenome.curation_api.model.entities.GeneToGeneParalogy;
import org.alliancegenome.es.index.ESDocument;

import lombok.Data;

@Data
public class GeneToGeneParalogyDocument extends ESDocument {

	String category = "gene_to_gene_paralogy";
	@Override
	public String getType() {
		return category;
	}

	GeneToGeneParalogy geneToGeneParalogy;
	
}
