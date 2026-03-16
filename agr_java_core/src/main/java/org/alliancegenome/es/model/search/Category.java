package org.alliancegenome.es.model.search;

public enum Category {

	ALLELE_SUMMARY("allele_summary", false),
	ALLELE_VARIANT("allele_variant_search_results", true),
	VARIANT("variant_search_results", true),
	SEQUENCE_SUMMARY("sequence_summary", false),
	DISEASE("disease", true),
	MODEL("model", true),
	GENE("gene", true),
	GO("go_search_result", true),
	DATASET("htp_dataset_search_result", true);

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
