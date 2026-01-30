package org.alliancegenome.api.entity;

import org.alliancegenome.curation_api.model.document.es.ESDocument;
import org.alliancegenome.curation_api.model.entities.Gene;
import org.alliancegenome.curation_api.view.CurationView;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonView;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonPropertyOrder({ "category", "gene", "allele", "transgenicAlleleConstructs", "geneList" })
@JsonView(value = { CurationView.TransgenicAllelesDocument.class })
public class GeneTransgenicAlleleSummaryDocument extends ESDocument {

	{
		category = "transgenic_allele_annotations";
	}

	private Gene gene;
	private TransgenicAlleleSummaryDocument alleleDocument;

	
	public GeneTransgenicAlleleSummaryDocument(Gene gene) {
		this.gene = gene;
	}

}
