package org.alliancegenome.indexer.config;

import org.alliancegenome.indexer.indexers.LiteratureIndexer;
import org.alliancegenome.indexer.indexers.ModelIndexer;
import org.alliancegenome.indexer.indexers.curation.AffectedGenomicModelCurationIndexer;
import org.alliancegenome.indexer.indexers.curation.AlleleSummaryCurationIndexer;
import org.alliancegenome.indexer.indexers.curation.DiseaseAnnotationCurationIndexer;
import org.alliancegenome.indexer.indexers.curation.DiseaseSearchResultCurationIndexer;
import org.alliancegenome.indexer.indexers.curation.DiseaseSummaryCurationIndexer;
import org.alliancegenome.indexer.indexers.curation.GOSearchResultCurationIndexer;
import org.alliancegenome.indexer.indexers.curation.GeneExpressionAnnotationIndexer;
import org.alliancegenome.indexer.indexers.curation.GeneExpressionRibbonSummaryIndexer;
import org.alliancegenome.indexer.indexers.curation.GeneGeneticInteractionCurationIndexer;
import org.alliancegenome.indexer.indexers.curation.GeneMolecularInteractionCurationIndexer;
import org.alliancegenome.indexer.indexers.curation.GeneSearchResultCurationIndexer;
import org.alliancegenome.indexer.indexers.curation.GeneSummaryCurationIndexer;
import org.alliancegenome.indexer.indexers.curation.GeneToGeneOrthologyIndexer;
import org.alliancegenome.indexer.indexers.curation.GeneToGeneParalogyIndexer;
import org.alliancegenome.indexer.indexers.curation.HTPDatasetSearchResultCurationIndexer;
import org.alliancegenome.indexer.indexers.curation.PhenotypeAnnotationCurationIndexer;
import org.alliancegenome.indexer.indexers.curation.ReleaseInfoIndexer;
import org.alliancegenome.indexer.indexers.curation.SiteMapAccessionCurationIndexer;
import org.alliancegenome.indexer.indexers.curation.TransgenicAlleleCurationIndexer;
import org.alliancegenome.indexer.indexers.curation.VariantSummaryCurationIndexer;

public enum IndexerConfig {

	// Neo Indexers
	ModelIndexer("model", ModelIndexer.class, 4, 1500, 1426, 4, 1, true),

	// Curation Indexers

	// Run Parallelly
	ParalogyIndexer("paralogy", GeneToGeneParalogyIndexer.class, 4, 5000, 5000, 8, 1, true),
	AffectedGenomicModelIndexer("affectedGenomicModels", AffectedGenomicModelCurationIndexer.class, 4, 1500, 1500, 8, 1, true),
	GeneSearchResultCurationIndexer("geneSearchResult", GeneSearchResultCurationIndexer.class, 4, 1000, 1500, 4, 1, true),
	GeneSummaryIndexer("geneSummary", GeneSummaryCurationIndexer.class, 4, 1500, 1500, 4, 1, true),
	VariantSummaryIndexer("variantSummary", VariantSummaryCurationIndexer.class, 1, 3000, 1500, 8, 1, false),
	GeneGeneticInteractionIndexers("geneGeneticInteraction", GeneGeneticInteractionCurationIndexer.class, 4, 1500, 1500, 2, 1, true),
	HTPDatasetSearchResultCurationIndexer("htpDatasetSearchResult", HTPDatasetSearchResultCurationIndexer.class, 4, 1500, 1500, 4, 1, true),
	LiteratureIndexer("literature", LiteratureIndexer.class, 4, 5000, 5000, 1, 1, true),
	GeneExpressionAnnotationIndexer("geneExpressionAnnotation", GeneExpressionAnnotationIndexer.class, 8, 125, 2000, 4, 1, true),
	SiteMapAccessionCurationIndexer("sitemap", SiteMapAccessionCurationIndexer.class, 4, 1500, 1500, 8, 1, true),
	DiseaseSummaryIndexer("diseaseSummary", DiseaseSummaryCurationIndexer.class, 4, 1500, 1500, 4, 1, true),
	TransgenicAlleleIndexer("transgenicAlleles", TransgenicAlleleCurationIndexer.class, 1, 3000, 1500, 8, 1, true),
	GeneExpressionRibbonSummaryIndexer("geneExpressionRibbonSummary", GeneExpressionRibbonSummaryIndexer.class, 1, 1, 1, 1, 1, true),
	ReleaseInfoIndexer("release", ReleaseInfoIndexer.class, 1, 1, 1, 1, 1, true),

	// Run Sequentially -- typically take a lot of RAM or do consolidation in memory
	DiseaseSearchResultIndexer("diseaseSearchResult", DiseaseSearchResultCurationIndexer.class, 8, 50, 50, 4, 1, false),
	GOSearchResultCurationIndexer("goSearchResult", GOSearchResultCurationIndexer.class, 4, 1500, 1500, 4, 1, false),
	AlleleSummaryIndexer("alleleSummary", AlleleSummaryCurationIndexer.class, 4, 1500, 1500, 4, 1, false),
	GeneToGeneOrthologyIndexer("geneToGeneOrthology", GeneToGeneOrthologyIndexer.class, 4, 2500, 2500, 8, 1, false),
	GeneMolecularInteractionIndexers("geneMolecularInteraction", GeneMolecularInteractionCurationIndexer.class, 4, 1500, 1500, 2, 1, false),
	PhenotypeAnnotationIndexer("phenotypeAnnotation", PhenotypeAnnotationCurationIndexer.class, 4, 1500, 1500, 2, 1, false),
	DiseaseAnnotationIndexer("diseaseAnnotation", DiseaseAnnotationCurationIndexer.class, 1, 1500, 1500, 2, 1, false),
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
