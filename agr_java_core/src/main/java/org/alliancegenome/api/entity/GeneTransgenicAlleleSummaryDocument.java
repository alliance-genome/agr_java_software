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
@JsonView(value = {org.alliancegenome.curation_api.view.View.TransgenicAllelesDocument.class})
public class GeneTransgenicAlleleSummaryDocument extends ESDocument {

	{
		category = "transgenic_allele_annotations";
	}

	public GeneTransgenicAlleleSummaryDocument(Gene gene) {
		this.gene = gene;
	}

	public GeneTransgenicAlleleSummaryDocument() {
	}

	private Gene gene;

	private TransgenicAlleleSummaryDocument alleleDocument;

}
