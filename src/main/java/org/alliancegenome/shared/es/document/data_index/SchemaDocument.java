package org.alliancegenome.shared.es.document.data_index;


import org.alliancegenome.shared.es.document.site_index.ESDocument;

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
