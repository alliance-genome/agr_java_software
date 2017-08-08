package org.alliancegenome.indexer.mapping;

import java.util.HashMap;

import lombok.Data;

@Data
public class Property {

	private String type;
	private String analyzer;
	private String search_analyzer;
	
	private HashMap<String, Property> fields = new HashMap<>();
	private HashMap<String, Property> properties = new HashMap<>();
}
