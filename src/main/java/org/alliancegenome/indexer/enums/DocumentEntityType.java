package org.alliancegenome.indexer.enums;

import org.alliancegenome.indexer.indexers.DiseaseIndexer;
import org.alliancegenome.indexer.indexers.GeneIndexer;
import org.alliancegenome.indexer.indexers.GoIndexer;

public enum DocumentEntityType {

	GENE(GeneIndexer.class, Index.GENE),
	DISEASE(DiseaseIndexer.class, Index.DISEASE),
	GO(GoIndexer.class, Index.GO)
	;

	private Class indexerClass;
	private Index index;

	private DocumentEntityType(Class indexerClass, Index index) {
		this.indexerClass = indexerClass;
		this.index = index;
	}
	
	public Class getIndexerClass() {
		return this.indexerClass;
	}
	
	public enum Index {
		GENE, GO, SEARCHABLE_ITEMS, DISEASE;
		
		public String getName() {
			return this.name().toLowerCase();
		}
		
	}
}
