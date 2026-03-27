package org.alliancegenome.es.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonView;

import org.alliancegenome.curation_api.model.document.es.ESDocument;
import org.alliancegenome.curation_api.view.CurationView;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonView({CurationView.VariantSearchResultDocument.class})
public class VariantSearchResultDocument extends ESDocument {

	public VariantSearchResultDocument() {
		category = "variant_search_result";
	}

	private boolean searchable;
	private String name;
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
