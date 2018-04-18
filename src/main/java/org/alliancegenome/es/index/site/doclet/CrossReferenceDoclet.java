package org.alliancegenome.es.index.site.doclet;

import org.alliancegenome.es.index.doclet.Doclet;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CrossReferenceDoclet extends Doclet {

	private String crossRefCompleteUrl;
	private String localId;
	private String globalCrossRefId;
	private String prefix;
	private String name;
	private String displayName;
	private String type;

}
