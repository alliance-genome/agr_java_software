package org.alliancegenome.indexer.document;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;
import lombok.ToString;

@Data
@ToString(includeFieldNames=true)
public class DiseaseDocument extends ESDocument {
	
	private String primaryId;
	
	@JsonIgnore
	public String getId() {
		return primaryId;
	}
}
