package org.alliancegenome.api.entity;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.alliancegenome.curation_api.model.document.es.ESDocument;
import org.alliancegenome.curation_api.model.entities.Gene;
import org.alliancegenome.neo4j.view.View;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonPropertyOrder({"category", "gene", "allele", "transgenicAlleleConstructs", "geneList"})
@JsonView({View.TransgenicAlleleAPI.class})
public class GeneTransgenicAlleleSummaryDocument extends ESDocument {

	public GeneTransgenicAlleleSummaryDocument(Gene gene) {
		setCategory("transgenic_allele_annotations");
		this.gene = gene;
	}

	private Gene gene;

	private TransgenicAlleleSummaryDocument alleleDocument;

}
