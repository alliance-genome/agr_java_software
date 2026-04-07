package org.alliancegenome.es.model;

import java.util.List;
import java.util.Set;

import org.alliancegenome.curation_api.model.document.es.ESDocument;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AlleleSearchResultDocument extends ESDocument {

	public AlleleSearchResultDocument() {
		category = "allele_search_result";
	}

	private boolean searchable;
	private String symbol;
	private String symbolText;
	private String name;
	private String nameKey;
	private String primaryKey;
	private String species;
	private Double popularity;
	private String alterationType;
	private List<String> variantType;
	private Set<String> molecularConsequence;
	private Set<String> variantSynonym;
	private Set<String> constructExpressedComponent;
	private Set<String> constructRegulatoryRegion;
	private Set<String> constructKnockdownComponent;
	private List<String> synonyms;
	private Set<String> geneSynonyms;
	private List<String> secondaryIds;
	private List<String> genes;
	private List<String> diseases;
	private List<String> diseasesAgrSlim;
	private List<String> diseasesWithParents;
	private List<String> phenotypeStatements;
	private String globalId;
	private String localId;
	private String modCrossRefCompleteUrl;

}
