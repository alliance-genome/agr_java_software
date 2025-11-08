package org.alliancegenome.api.entity;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.alliancegenome.curation_api.model.document.es.ESDocument;
import org.alliancegenome.curation_api.model.entities.Allele;
import org.alliancegenome.curation_api.model.entities.TransgenicAlleleConstruct;
import org.alliancegenome.neo4j.view.View;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonPropertyOrder({"category", "allele", "transgenicAlleleConstructs", "geneList"})
@JsonView(value = {org.alliancegenome.curation_api.view.View.TransgenicAllelesDocument.class})
public class TransgenicAlleleSummaryDocument extends ESDocument {

	public TransgenicAlleleSummaryDocument() {
		setCategory("transgenic_allele_summary");
	}

	private Allele allele;
	public List<TransgenicAlleleConstruct> transgenicAlleleConstructs;
	private Boolean hasDiseaseAnnotations;
	private Boolean hasPhenotypeAnnotations;
	private int phylogeneticSortingIndex;



}
