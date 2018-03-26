package org.alliancegenome.es.index.data.document;


import org.alliancegenome.es.index.site.document.ESDocument;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter @ToString
public class SchemaDocument extends ESDocument {

	private String type = "schema";
	private String name;

	@Override
	public String getDocumentId() {
		return name;
	}

}
