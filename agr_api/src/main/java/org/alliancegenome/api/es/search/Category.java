package org.alliancegenome.api.es.search;

public enum Category {

	ALLELE_SUMMARY("allele_summary"),
	ALLELE("allele_search_result"),
	VARIANT("variant_search_result"),
	SEQUENCE_SUMMARY("sequence_summary"),
	DISEASE("disease_search_result"),
	MODEL("model_search_result"),
	GENE("gene_search_result"),
	GO("go_search_result"),
	DATASET("htp_dataset_search_result"),
	LITERATURE("literature_search_result");

	private String name;

	private Category(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

}
