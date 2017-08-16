package org.alliancegenome.indexer.document.searchableitem;

import org.alliancegenome.indexer.document.ESDocument;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public abstract class SearchableItemDocument extends ESDocument {

	private String category;
	private String href;
	private String name;
	private String name_key;
	private String description;
	
}
