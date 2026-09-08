package org.alliancegenome.core.document;

import java.util.Set;

import org.alliancegenome.curation_api.model.document.es.ESDocument;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AlleleSearchResultDocument extends ESDocument {
	{
		category = "allele_search_result";
		searchable = true;
	}

	private String symbol;
	private String symbolText;
	private String name;
	private String nameKey;
	private String primaryKey;
	private String species;
	private Double popularity;
	private String alterationType;
	private String globalId;
	private String localId;
	private String modCrossRefCompleteUrl;
	private String systematicName;

	private Set<String> variantType;
	private Set<String> molecularConsequence;
	private Set<String> variants;
	private Set<String> constructs;
	private Set<String> constructExpressedComponent;
	private Set<String> constructRegulatoryRegion;
	private Set<String> constructKnockdownComponent;
	private Set<String> crossReferences;
	private Set<String> geneCrossReferences;
	private Set<String> synonyms;
	private Set<String> geneSynonyms;
	private Set<String> secondaryIds;
	private Set<String> genes;
	private Set<String> diseases;
	private Set<String> diseasesAgrSlim;
	private Set<String> diseasesWithParents;
	private Set<String> phenotypeStatements;

}
