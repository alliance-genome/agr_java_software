package org.alliancegenome.indexer.document;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter @AllArgsConstructor
public class CrossReferenceDocument extends ESDocument {
	private String crossrefCompleteUrl;
	private String localId;
	private String id;
	private String globalCrossrefId;
	
	@Override
	public String getDocumentId() {
		return id;
	}
	
}
