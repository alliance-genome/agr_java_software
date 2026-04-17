package org.alliancegenome.es.model;

import java.util.List;
import java.util.Set;

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
	private String alterationType;
	
	private Set<String> alleles;
	private Set<String> genes;
	private Set<String> geneSynonyms;
	private Set<String> geneCrossReferences;
	private Set<String> secondaryIds;
	private Set<String> variantType;
	private Set<String> molecularConsequence;
	private Set<String> crossReferences;

}
