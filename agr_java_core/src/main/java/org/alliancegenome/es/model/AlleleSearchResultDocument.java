package org.alliancegenome.es.model;

import java.util.List;
import java.util.Set;

import org.alliancegenome.curation_api.model.document.es.ESDocument;
import org.alliancegenome.es.model.search.RelatedDataLink;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AlleleSearchResultDocument extends ESDocument {

	public AlleleSearchResultDocument() {
		category = "allele_variant_search_result";
	}

	private boolean searchable;
	private String symbol;
	private String symbolText;
	private String name;
	@JsonProperty("name_key")
	private String nameKey;
	private String primaryKey;
	private String species;
	private Double popularity;
	private String alterationType;
	private List<String> variantTypes;
	private Set<String> molecularConsequences;
	private List<String> synonyms;
	private List<String> secondaryIds;
	private List<String> genes;
	private List<String> diseases;
	private List<String> diseasesAgrSlim;
	private List<String> phenotypeStatements;
	private String globalId;
	private String localId;
	private String modCrossRefCompleteUrl;
	private List<RelatedDataLink> relatedData;

}
