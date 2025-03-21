package org.alliancegenome.indexer.config;

import org.alliancegenome.indexer.indexers.AlleleIndexer;
import org.alliancegenome.indexer.indexers.DatasetIndexer;
import org.alliancegenome.indexer.indexers.DiseaseIndexer;
import org.alliancegenome.indexer.indexers.GeneIndexer;
import org.alliancegenome.indexer.indexers.GoIndexer;
import org.alliancegenome.indexer.indexers.ModelIndexer;
import org.alliancegenome.indexer.indexers.curation.DiseaseAnnotationCurationIndexer;
import org.alliancegenome.indexer.indexers.curation.DiseaseSummaryCurationIndexer;
import org.alliancegenome.indexer.indexers.curation.GeneExpressionAnnotationIndexer;
import org.alliancegenome.indexer.indexers.curation.GeneGeneticInteractionCurationIndexer;
import org.alliancegenome.indexer.indexers.curation.GeneMolecularInteractionCurationIndexer;
import org.alliancegenome.indexer.indexers.curation.GeneSearchResultCurationIndexer;
import org.alliancegenome.indexer.indexers.curation.GeneToGeneOrthologyIndexer;
import org.alliancegenome.indexer.indexers.curation.GeneToGeneParalogyIndexer;
import org.alliancegenome.indexer.indexers.curation.PhenotypeAnnotationCurationIndexer;
import org.alliancegenome.indexer.indexers.curation.ReleaseInfoIndexer;

public enum IndexerConfig {
	
	// Neo Indexers
	GeneIndexer("gene", GeneIndexer.class, 4, 359, 359, 8, 1),
	DatasetIndexer("dataset", DatasetIndexer.class, 4, 566, 566, 8, 1),
	DiseaseIndexer("disease", DiseaseIndexer.class, 4, 680, 680, 8, 1),
	AlleleIndexer("allele", AlleleIndexer.class, 4, 1517, 1517, 8, 1),
	GoIndexer("go", GoIndexer.class, 4, 914, 914, 8, 1),
	ModelIndexer("model", ModelIndexer.class, 4, 1426, 1426, 8, 1),
	
	// Curation Indexers
	DiseaseAnnotationIndexer("diseaseAnnotation", DiseaseAnnotationCurationIndexer.class, 1, 1500, 1500, 2, 1),
	GeneExpressionAnnotationIndexer("geneExpressionAnnotation", GeneExpressionAnnotationIndexer.class, 4, 1500, 1500, 2, 1),
	GeneGeneticInteractionIndexers("geneGeneticInteraction", GeneGeneticInteractionCurationIndexer.class, 4, 1500, 1500, 2, 1),
	GeneMolecularInteractionIndexers("geneMolecularInteraction", GeneMolecularInteractionCurationIndexer.class, 4, 1500, 1500, 2, 1),
	ParalogyIndexer("paralogy", GeneToGeneParalogyIndexer.class, 4, 5000, 5000, 8, 1),
	PhenotypeAnnotationIndexer("phenotypeAnnotation", PhenotypeAnnotationCurationIndexer.class, 4, 1500, 1500, 2, 1),
	ReleaseInfoIndexer("release", ReleaseInfoIndexer.class, 1, 1, 1, 1, 1),
	DiseaseSummaryIndexer("diseaseSummary", DiseaseSummaryCurationIndexer.class, 4, 1500, 1500, 4, 1),
	GeneToGeneOrthologyIndexer("geneToGeneOrthology", GeneToGeneOrthologyIndexer.class, 4, 500, 500, 8, 1),
	GeneSearchResultCurationIndexer("geneSearchResult", GeneSearchResultCurationIndexer.class, 4, 250, 1000, 4, 1),
	;


	private String typeName;
	private Class<?> indexClazz;
	private int threadCount;
	private int bufferSize;
	private int bulkActions;
	private int concurrentRequests;
	private int bulkSize;

	IndexerConfig(String typeName, Class<?> indexClazz, int threadCount, int bufferSize, int bulkActions, int concurrentRequests, int bulkSize) {
		this.typeName = typeName;
		this.indexClazz = indexClazz;
		this.threadCount = threadCount;
		this.bufferSize = bufferSize;
		this.bulkActions = bulkActions;
		this.concurrentRequests = concurrentRequests;
		this.bulkSize = bulkSize;
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

	public int getBufferSize() {
		return bufferSize;
	}

	public int getBulkActions() {
		return bulkActions;
	}

	public int getConcurrentRequests() {
		return concurrentRequests;
	}

	public int getBulkSize() {
		return bulkSize;
	}

}
