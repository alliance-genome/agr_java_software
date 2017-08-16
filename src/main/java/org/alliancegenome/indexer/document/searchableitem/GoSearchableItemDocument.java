package org.alliancegenome.indexer.document.searchableitem;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class GoSearchableItemDocument extends SearchableItemDocument {

	private String id;
	private String go_type;
	private List<String> go_synonyms;
	private List<String> go_genes;
	private List<String> go_species;

	@JsonIgnore
	public String getDocumentId() {
		return id;
	}
}
