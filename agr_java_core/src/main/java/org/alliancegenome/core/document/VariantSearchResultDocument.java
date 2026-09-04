package org.alliancegenome.core.document;

import java.util.Set;

import org.alliancegenome.curation_api.model.document.es.ESDocument;
import org.alliancegenome.curation_api.view.CurationView;

import com.fasterxml.jackson.annotation.JsonView;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonView({CurationView.VariantSearchResultDocument.class})
public class VariantSearchResultDocument extends ESDocument {
	{
		category = "variant_search_result";
		searchable = true;
	}

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
	private Set<String> systematicName;
	private Set<String> secondaryIds;
	private Set<String> variantType;
	private Set<String> molecularConsequence;
	private Set<String> crossReferences;

}
