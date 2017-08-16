package org.alliancegenome.indexer.document.searchableitem;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;
import lombok.ToString;

@Data
@ToString(includeFieldNames=true)
public class GoSearchableItemDocument extends SearchableItemDocument {

	private String id;
	private List<String> go_synonyms;
	private List<String> go_genes;
	private List<String> go_species;
	private String go_type;

	@JsonIgnore
	public String getDocumentId() {
		return id;
	}
}
