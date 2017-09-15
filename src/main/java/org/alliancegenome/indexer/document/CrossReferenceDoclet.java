package org.alliancegenome.indexer.document;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CrossReferenceDoclet {

	private String crossRefCompleteUrl;
	private String localId;
	private String id;
	private String globalCrossrefId;
	private String prefix;
	private String displayName;

}
