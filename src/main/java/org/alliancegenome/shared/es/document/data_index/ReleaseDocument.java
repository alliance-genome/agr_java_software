package org.alliancegenome.shared.es.document.data_index;

import org.alliancegenome.shared.es.document.site_index.ESDocument;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter @ToString
public class ReleaseDocument extends ESDocument {

	private String type = "release";

	private String name;

	@Override
	public String getDocumentId() {
		return name;
	}

}
