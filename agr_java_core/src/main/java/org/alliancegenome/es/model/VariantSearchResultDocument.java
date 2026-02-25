package org.alliancegenome.es.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonView;
import org.alliancegenome.curation_api.model.document.es.ESDocument;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;
import org.alliancegenome.curation_api.view.CurationView;

@Getter
@Setter
@JsonView({CurationView.FieldsAndLists.class})
public class VariantSearchResultDocument extends ESDocument {

	public VariantSearchResultDocument() {
		category = "variant_search_result";
	}

	private boolean searchable;
	private String name;
	@JsonProperty("name_key")
	private String nameKey;
	private String primaryKey;
	private String species;
	private Double popularity;
	private List<String> alleles;
	private List<String> genes;
	private String alterationType;
	private List<String> variantType;
	private List<String> molecularConsequence;
	private List<String> crossReferences;

}
