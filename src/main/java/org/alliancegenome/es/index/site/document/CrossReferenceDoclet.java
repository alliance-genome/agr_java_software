package org.alliancegenome.es.index.site.document;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CrossReferenceDoclet {

	private String crossRefCompleteUrl;
	private String localId;
	private String globalCrossRefId;
	private String prefix;
	private String name;
	private String type;

}
