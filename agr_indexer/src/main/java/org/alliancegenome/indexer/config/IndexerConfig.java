package org.alliancegenome.indexer.config;

import org.alliancegenome.indexer.indexers.AGMAnnotationCurationIndexer;
import org.alliancegenome.indexer.indexers.AlleleSummaryCurationIndexer;
import org.alliancegenome.indexer.indexers.DiseaseAnnotationCurationIndexer;
import org.alliancegenome.indexer.indexers.DiseaseSearchResultCurationIndexer;
import org.alliancegenome.indexer.indexers.DiseaseSummaryCurationIndexer;
import org.alliancegenome.indexer.indexers.GOSearchResultCurationIndexer;
import org.alliancegenome.indexer.indexers.GeneExpressionAnnotationIndexer;
import org.alliancegenome.indexer.indexers.GeneExpressionRibbonSummaryIndexer;
import org.alliancegenome.indexer.indexers.GeneGeneticInteractionCurationIndexer;
import org.alliancegenome.indexer.indexers.GeneMolecularInteractionCurationIndexer;
import org.alliancegenome.indexer.indexers.GeneSearchResultCurationIndexer;
import org.alliancegenome.indexer.indexers.GeneSummaryCurationIndexer;
import org.alliancegenome.indexer.indexers.GeneToGeneOrthologyIndexer;
import org.alliancegenome.indexer.indexers.GeneToGeneParalogyIndexer;
import org.alliancegenome.indexer.indexers.HTPDatasetSearchResultCurationIndexer;
import org.alliancegenome.indexer.indexers.LiteratureIndexer;
import org.alliancegenome.indexer.indexers.ModelSearchResultCurationIndexer;
import org.alliancegenome.indexer.indexers.PhenotypeAnnotationCurationIndexer;
import org.alliancegenome.indexer.indexers.ReleaseInfoIndexer;
import org.alliancegenome.indexer.indexers.SiteMapAccessionCurationIndexer;
import org.alliancegenome.indexer.indexers.TransgenicAlleleCurationIndexer;

public enum IndexerConfig {

	// Curation Indexers

	// Run Sequentially -- typically take a lot of RAM or do consolidation in memory
	ReleaseInfoIndexer("release", ReleaseInfoIndexer.class, 1, 1, 1, 1, 1, false),
	GeneExpressionRibbonSummaryIndexer("geneExpressionRibbonSummary", GeneExpressionRibbonSummaryIndexer.class, 1, 1, 1, 1, 1, false),
	
	// Run Parallelly
	ParalogyIndexer("paralogy", GeneToGeneParalogyIndexer.class, 4, 5000, 5000, 8, 1, true),
	AffectedGenomicModelAnnotationIndexer("agmAnnotation", AGMAnnotationCurationIndexer.class, 4, 1500, 1500, 8, 1, true),
	GeneSearchResultCurationIndexer("geneSearchResult", GeneSearchResultCurationIndexer.class, 4, 1000, 1500, 4, 1, true),
	GeneSummaryIndexer("geneSummary", GeneSummaryCurationIndexer.class, 4, 1500, 1500, 4, 1, true),
	//VariantSummaryIndexer("variantSummary", VariantSummaryCurationIndexer.class, 1, 3000, 1500, 8, 1, true),
	GeneGeneticInteractionIndexers("geneGeneticInteraction", GeneGeneticInteractionCurationIndexer.class, 4, 1500, 1500, 2, 1, true),
	HTPDatasetSearchResultCurationIndexer("htpDatasetSearchResult", HTPDatasetSearchResultCurationIndexer.class, 4, 1500, 1500, 4, 1, true),
	LiteratureIndexer("literature", LiteratureIndexer.class, 4, 7500, 7500, 4, 10, true),
	GeneExpressionAnnotationIndexer("geneExpressionAnnotation", GeneExpressionAnnotationIndexer.class, 8, 125, 2000, 4, 1, true),
	SiteMapAccessionCurationIndexer("sitemap", SiteMapAccessionCurationIndexer.class, 4, 1500, 1500, 8, 1, true),
	DiseaseSummaryIndexer("diseaseSummary", DiseaseSummaryCurationIndexer.class, 4, 1500, 1500, 4, 1, true),
	TransgenicAlleleIndexer("transgenicAlleles", TransgenicAlleleCurationIndexer.class, 1, 3000, 1500, 8, 1, true),
	DiseaseSearchResultIndexer("diseaseSearchResult", DiseaseSearchResultCurationIndexer.class, 8, 50, 50, 4, 1, true),
	GOSearchResultCurationIndexer("goSearchResult", GOSearchResultCurationIndexer.class, 4, 1500, 1500, 4, 1, true),
	ModelSearchResultIndexer("modelSearchResult", ModelSearchResultCurationIndexer.class, 4, 1500, 1500, 4, 1, true),
	AlleleSummaryIndexer("alleleSummary", AlleleSummaryCurationIndexer.class, 8, 1500, 1500, 8, 10, true),
	GeneToGeneOrthologyIndexer("geneToGeneOrthology", GeneToGeneOrthologyIndexer.class, 4, 2500, 2500, 8, 1, true),
	GeneMolecularInteractionIndexers("geneMolecularInteraction", GeneMolecularInteractionCurationIndexer.class, 4, 1500, 1500, 2, 1, true),
	PhenotypeAnnotationIndexer("phenotypeAnnotation", PhenotypeAnnotationCurationIndexer.class, 4, 1500, 1500, 2, 1, true),
	DiseaseAnnotationIndexer("diseaseAnnotation", DiseaseAnnotationCurationIndexer.class, 1, 1500, 1500, 2, 1, true),
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
