package org.alliancegenome.es.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.alliancegenome.curation_api.model.document.es.ESDocument;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AlleleSearchResultDocument extends ESDocument {

	public AlleleSearchResultDocument() {
		category = "allele";
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
	private List<String> synonyms;
	private List<String> secondaryIds;
	private List<String> genes;
	private List<String> diseases;
	private List<String> phenotypeStatements;
	private String globalId;
	private String localId;
	private String modCrossRefCompleteUrl;

}
