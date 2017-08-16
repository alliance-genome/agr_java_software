package org.alliancegenome.indexer.document.searchableitem;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class DiseaseSearchableItemDocument extends SearchableItemDocument {
	
	private String doUrl;
	private String doDisplayId;
	private String doId;
	private String doPrefix;
	private String primaryKey;
	private String id;
	
	@JsonIgnore
	public String getDocumentId() {
		return id;
	}

}
