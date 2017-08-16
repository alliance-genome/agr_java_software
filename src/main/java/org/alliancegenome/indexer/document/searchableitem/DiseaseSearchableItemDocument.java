package org.alliancegenome.indexer.document.searchableitem;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class DiseaseSearchableItemDocument extends SearchableItemDocument {
	
	private String id;
	
	@JsonIgnore
	public String getDocumentId() {
		return id;
	}

}
