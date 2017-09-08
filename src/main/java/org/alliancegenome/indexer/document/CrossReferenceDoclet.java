package org.alliancegenome.indexer.document;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter @AllArgsConstructor
public class CrossReferenceDoclet {
	private String crossrefCompleteUrl;
	private String localId;
	private String id;
	private String globalCrossrefId;
	
}
