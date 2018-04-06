package org.alliancegenome.es.index.data.document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.alliancegenome.es.index.document.ESDocument;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter @ToString
public class MetaDataDocument extends ESDocument {

	private List<String> schemas = new ArrayList<String>();
	
	private HashMap<String, String> releaseSchemaMap = new HashMap<>();
	
	private HashMap<String, DataTypeDoclet> dataTypes = new HashMap<>();
	private HashMap<String, TaxonIdDoclet> taxonIds = new HashMap<>();
	
	@Override
	public String getDocumentId() {
		return "meta_data_id";
	}

	@Override
	public String getType() {
		return "meta_data";
	}

}
