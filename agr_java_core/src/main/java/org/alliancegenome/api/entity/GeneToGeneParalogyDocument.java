package org.alliancegenome.api.entity;

import org.alliancegenome.curation_api.model.document.es.ESDocument;
import org.alliancegenome.curation_api.model.entities.GeneToGeneParalogy;

import lombok.Data;

@Data
public class GeneToGeneParalogyDocument extends ESDocument {
	{
		category = "gene_to_gene_paralogy";
	}
	private GeneToGeneParalogy geneToGeneParalogy;
}
