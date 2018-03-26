package org.alliancegenome.es.index.data.document;

import org.alliancegenome.es.index.site.document.ESDocument;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter @ToString
public class TaxonIdDocument extends ESDocument {

	private String type = "taxonid";

	private String taxonId;
	private String modName;
	private String description;

	@Override
	public String getDocumentId() {
		return taxonId;
	}

}
