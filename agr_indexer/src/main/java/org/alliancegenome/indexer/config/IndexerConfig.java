package org.alliancegenome.indexer.config;

import org.alliancegenome.indexer.indexers.AlleleIndexer;
import org.alliancegenome.indexer.indexers.DatasetIndexer;
import org.alliancegenome.indexer.indexers.DiseaseIndexer;
import org.alliancegenome.indexer.indexers.GeneIndexer;
import org.alliancegenome.indexer.indexers.GoIndexer;
import org.alliancegenome.indexer.indexers.ModelIndexer;
import org.alliancegenome.indexer.indexers.curation.DiseaseAnnotationCurationIndexer;

public enum IndexerConfig {

	GeneIndexer("gene", GeneIndexer.class, 4),
	DatasetIndexer("dataset", DatasetIndexer.class, 4),
	DiseaseIndexer("disease", DiseaseIndexer.class, 4),
	AlleleIndexer("allele", AlleleIndexer.class, 4),
	GoIndexer("go", GoIndexer.class, 4),
	ModelIndexer("model", ModelIndexer.class, 4),
	DiseaseAnnotationMlIndexer("diseaseAnnotation", DiseaseAnnotationCurationIndexer.class, 4),	
	// still not implemented  VariantIndexer("variant", VariantIndexer.class, 2)
	;

	private String typeName;
	private Class<?> indexClazz;
	private int threadCount;

	IndexerConfig(String typeName, Class<?> indexClazz, int threadCount) {
		this.typeName = typeName;
		this.indexClazz = indexClazz;
		this.threadCount = threadCount;
	}

	public String getTypeName() {
		return typeName;
	}

	public Class<?> getIndexClazz() {
		return indexClazz;
	}

	public int getThreadCount() {
		return threadCount;
	}
}
