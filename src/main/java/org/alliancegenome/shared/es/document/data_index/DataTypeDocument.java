package org.alliancegenome.shared.es.document.data_index;

import java.util.HashMap;

import org.alliancegenome.shared.es.document.site_index.ESDocument;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter @ToString
public class DataTypeDocument extends ESDocument {

	private String type = "data_type";

	private String name;
	private String fileExtension;
	private String description;
	private boolean taxonIdRequired;
	private boolean validationRequired;
	private HashMap<String, String> schemaFiles = new HashMap<String, String>();

	@Override
	public String getDocumentId() {
		return name;
	}

}
