package org.alliancegenome.core.document;

import java.util.List;

import org.alliancegenome.curation_api.model.document.es.ESDocument;
import org.alliancegenome.curation_api.model.entities.Allele;
import org.alliancegenome.curation_api.model.entities.TransgenicAlleleConstruct;
import org.alliancegenome.curation_api.view.CurationView;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonView;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonPropertyOrder({ "category", "allele", "transgenicAlleleConstructs", "geneList" })
@JsonView(value = { CurationView.TransgenicAllelesDocument.class })
public class TransgenicAlleleSummaryDocument extends ESDocument {

	{
		category = "transgenic_allele_summary";
	}
	
	private Allele allele;
	public List<TransgenicAlleleConstruct> transgenicAlleleConstructs;
	private Boolean hasDiseaseAnnotations;
	private Boolean hasPhenotypeAnnotations;
	private int phylogeneticSortingIndex;

}
