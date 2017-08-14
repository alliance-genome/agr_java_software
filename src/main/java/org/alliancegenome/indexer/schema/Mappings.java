package org.alliancegenome.indexer.schema;

public abstract class Mappings extends Builder {
	
	public Mappings(Boolean pretty) {
		super(pretty);
	}

	public abstract void buildMappings(boolean enclose);
	
}
