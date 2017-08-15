package org.alliancegenome.indexer.document.searchableitem;

import org.alliancegenome.indexer.document.ESDocument;

import lombok.Data;
import lombok.ToString;

@Data
@ToString(includeFieldNames=true)
public abstract class SearchableItemDocument extends ESDocument {

	private String category;
	private String href;
	private String name;
	private String name_key;
	private String description;
	
}
