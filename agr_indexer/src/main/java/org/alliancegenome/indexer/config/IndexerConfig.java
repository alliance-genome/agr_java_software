package org.alliancegenome.indexer.config;

import org.alliancegenome.indexer.indexers.*;
import org.alliancegenome.indexer.indexers.curation.*;

public enum IndexerConfig {

	// Neo Indexers
	GeneIndexer("gene", GeneIndexer.class, 4, 359, 359, 8, 1, false),
	//DatasetIndexer("dataset", DatasetIndexer.class, 4, 566, 566, 8, 1, false), // Disabled: Dataset indexing now handled by HTPDatasetSearchResultCurationIndexer
	DiseaseIndexer("disease", DiseaseIndexer.class, 4, 680, 680, 8, 1, false),
	AlleleIndexer("allele", AlleleIndexer.class, 4, 1517, 1517, 8, 1, false),
	//GoIndexer("go", GoIndexer.class, 4, 914, 914, 8, 1, false), // Disabled: GO indexing now handled by GOSearchResultCurationIndexer
	ModelIndexer("model", ModelIndexer.class, 4, 1500, 1426, 4, 1, false),

	// Curation Indexers

	// Run Parallelly
	ParalogyIndexer("paralogy", GeneToGeneParalogyIndexer.class, 4, 5000, 5000, 8, 1, true),
	GOSearchResultCurationIndexer("goSearchResult", GOSearchResultCurationIndexer.class, 4, 1500, 1500, 4, 1, true),
	HTPDatasetSearchResultCurationIndexer("htpDatasetSearchResult", HTPDatasetSearchResultCurationIndexer.class, 4, 1500, 1500, 4, 1, true),
	LiteratureIndexer("literature", LiteratureIndexer.class, 4, 5000, 5000, 1, 1, true),
	GeneGeneticInteractionIndexers("geneGeneticInteraction", GeneGeneticInteractionCurationIndexer.class, 4, 1500, 1500, 2, 1, true),
	AffectedGenomicModelIndexer("affectedGenomicModels", AffectedGenomicModelCurationIndexer.class, 4, 1500, 1500, 8, 1, true),
	DiseaseSummaryIndexer("diseaseSummary", DiseaseSummaryCurationIndexer.class, 4, 1500, 1500, 4, 1, true),
	DiseaseSearchResultIndexer("diseaseSearchResult", DiseaseSearchResultCurationIndexer.class, 1, 15, 15, 4, 1, true),
	
	SiteMapAccessionCurationIndexer("sitemap", SiteMapAccessionCurationIndexer.class, 4, 1500, 1500, 8, 1, true),
	GeneExpressionRibbonSummaryIndexer("geneExpressionRibbonSummary", GeneExpressionRibbonSummaryIndexer.class, 1, 1, 1, 1, 1, true),
	ReleaseInfoIndexer("release", ReleaseInfoIndexer.class, 1, 1, 1, 1, 1, true),

	// Run Sequentially
	AlleleSummaryIndexer("alleleSummary", AlleleSummaryCurationIndexer.class, 4, 1500, 1500, 4, 1, false),
	GeneSummaryIndexer("geneSummary", GeneSummaryCurationIndexer.class, 4, 1500, 1500, 4, 1, false),
	GeneToGeneOrthologyIndexer("geneToGeneOrthology", GeneToGeneOrthologyIndexer.class, 4, 2500, 2500, 8, 1, false),

	GeneExpressionAnnotationIndexer("geneExpressionAnnotation", GeneExpressionAnnotationIndexer.class, 8, 125, 2000, 4, 1, false),

	PhenotypeAnnotationIndexer("phenotypeAnnotation", PhenotypeAnnotationCurationIndexer.class, 4, 1500, 1500, 2, 1, false),
	TransgenicAlleleIndexer("transgenicAlleles", TransgenicAlleleCurationIndexer.class, 1, 3000, 1500, 8, 1, false),
	GeneMolecularInteractionIndexers("geneMolecularInteraction", GeneMolecularInteractionCurationIndexer.class, 4, 1500, 1500, 2, 1, false),
	DiseaseAnnotationIndexer("diseaseAnnotation", DiseaseAnnotationCurationIndexer.class, 1, 1500, 1500, 2, 1, false),
	VariantSummaryIndexer("variantSummary", VariantSummaryCurationIndexer.class, 1, 3000, 1500, 8, 1, false),

	//GeneSearchResultCurationIndexer("geneSearchResult", GeneSearchResultCurationIndexer.class, 4, 250, 1000, 4, 1),

	;


	private String typeName;
	private Class<?> indexClazz;
	private int threadCount;
	private int bufferSize;
	private int bulkActions;
	private int concurrentRequests;
	private int bulkSize;
	private boolean runInParallel;

	IndexerConfig(String typeName, Class<?> indexClazz, int threadCount, int bufferSize, int bulkActions, int concurrentRequests, int bulkSize, boolean runInParallel) {
		this.typeName = typeName;
		this.indexClazz = indexClazz;
		this.threadCount = threadCount;
		this.bufferSize = bufferSize;
		this.bulkActions = bulkActions;
		this.concurrentRequests = concurrentRequests;
		this.bulkSize = bulkSize;
		this.runInParallel = runInParallel;
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

	public boolean getRunInParallel() {
		return runInParallel;
	}

}
