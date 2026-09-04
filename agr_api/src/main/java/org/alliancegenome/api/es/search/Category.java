package org.alliancegenome.api.es.search;

public enum Category {

	ALLELE_SUMMARY("allele_summary", false),
	ALLELE("allele_search_result", true),
	VARIANT("variant_search_result", true),
	SEQUENCE_SUMMARY("sequence_summary", false),
	DISEASE("disease_search_result", true),
	MODEL("model_search_result", true),
	GENE("gene_search_result", true),
	GO("go_search_result", true),
	DATASET("htp_dataset_search_result", true),
	LITERATURE("literature_search_result", true);

	private String name;
	private Boolean searchable;

	private Category(String name, Boolean searchable) {
		this.name = name;
		this.searchable = searchable;
	}

	public String getName() {
		return name;
	}

	public Boolean isSearchable() {
		return searchable;
	}
}
