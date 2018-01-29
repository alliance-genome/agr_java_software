package org.alliancegenome.shared.es.document.data_index;

import org.alliancegenome.shared.es.document.site_index.ESDocument;

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
