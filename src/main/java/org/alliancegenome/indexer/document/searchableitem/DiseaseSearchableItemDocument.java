package org.alliancegenome.indexer.document.searchableitem;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;
import lombok.ToString;

@Data
@ToString(includeFieldNames=true)
public class DiseaseSearchableItemDocument extends SearchableItemDocument {
	
	private String id;
	
	@JsonIgnore
	public String getDocumentId() {
		return id;
	}

}
