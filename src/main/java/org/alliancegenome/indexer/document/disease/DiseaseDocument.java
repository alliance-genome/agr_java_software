package org.alliancegenome.indexer.document.disease;

import org.alliancegenome.indexer.document.ESDocument;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class DiseaseDocument extends ESDocument {
	
	private String primaryId;
	
	@JsonIgnore
	public String getDocumentId() {
		return primaryId;
	}
}
