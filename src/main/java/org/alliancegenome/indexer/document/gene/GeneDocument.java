package org.alliancegenome.indexer.document.gene;

import org.alliancegenome.indexer.document.ESDocument;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;
import lombok.ToString;

@Data
@ToString(includeFieldNames=true)
public class GeneDocument extends ESDocument {
	
	private String primaryId;
	
	@JsonIgnore
	public String getDocumentId() {
		return primaryId;
	}
}
