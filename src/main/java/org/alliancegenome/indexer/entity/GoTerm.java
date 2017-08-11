package org.alliancegenome.indexer.entity;

import lombok.Data;

@Data
public class GOTerm extends Ontology {

	private String nameKey;
	private String name;
	private String description;
	private String href;
	private String type;
	private String primaryKey;
}
