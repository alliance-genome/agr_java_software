package org.alliancegenome.indexer.config;

import org.alliancegenome.core.es.util.IndexName;
import org.alliancegenome.indexer.indexers.AGMAnnotationIndexer;
import org.alliancegenome.indexer.indexers.AlleleSummaryIndexer;
import org.alliancegenome.indexer.indexers.DiseaseAnnotationIndexer;
import org.alliancegenome.indexer.indexers.DiseaseSearchResultIndexer;
import org.alliancegenome.indexer.indexers.DiseaseSummaryIndexer;
import org.alliancegenome.indexer.indexers.GOSearchResultIndexer;
import org.alliancegenome.indexer.indexers.GeneExpressionAnnotationIndexer;
import org.alliancegenome.indexer.indexers.GeneExpressionRibbonSummaryIndexer;
import org.alliancegenome.indexer.indexers.GeneGeneticInteractionIndexer;
import org.alliancegenome.indexer.indexers.GeneMolecularInteractionIndexer;
import org.alliancegenome.indexer.indexers.GeneSearchResultIndexer;
import org.alliancegenome.indexer.indexers.GeneSummaryIndexer;
import org.alliancegenome.indexer.indexers.GeneToGeneOrthologyIndexer;
import org.alliancegenome.indexer.indexers.GeneToGeneParalogyIndexer;
import org.alliancegenome.indexer.indexers.HTPDatasetSearchResultIndexer;
import org.alliancegenome.indexer.indexers.LiteratureIndexer;
import org.alliancegenome.indexer.indexers.ModelSearchResultIndexer;
import org.alliancegenome.indexer.indexers.PhenotypeAnnotationIndexer;
import org.alliancegenome.indexer.indexers.ReleaseInfoIndexer;
import org.alliancegenome.indexer.indexers.ResourceDescriptorIndexer;
import org.alliancegenome.indexer.indexers.SiteMapAccessionIndexer;
import org.alliancegenome.indexer.indexers.SpeciesIndexer;
import org.alliancegenome.indexer.indexers.TransgenicAlleleIndexer;

public enum IndexerConfig {

	// Curation Indexers

	// Run Sequentially -- typically take a lot of RAM or do consolidation in memory
	ReleaseInfo("release", ReleaseInfoIndexer.class, IndexName.SITE_INDEX, 1, 1, 1, 1, 1, false),
	ResourceDescriptor("resourceDescriptor", ResourceDescriptorIndexer.class, IndexName.SITE_INDEX, 1, 1, 1, 1, 1, false),
	Species("species", SpeciesIndexer.class, IndexName.SITE_INDEX, 1, 1, 1, 1, 1, false),
	GeneExpressionRibbonSummary("geneExpressionRibbonSummary", GeneExpressionRibbonSummaryIndexer.class, IndexName.SITE_INDEX, 1, 1, 1, 1, 1, false),
	
	// Run Parallelly
	PhenotypeAnnotation("phenotypeAnnotation", PhenotypeAnnotationIndexer.class, IndexName.SITE_INDEX, 8, 1500, 1500, 8, 10, true),
	GeneSummary("geneSummary", GeneSummaryIndexer.class, IndexName.SITE_INDEX, 8, 1500, 1500, 8, 10, true),
	AlleleSummary("alleleSummary", AlleleSummaryIndexer.class, IndexName.SITE_INDEX, 8, 1500, 1500, 8, 10, true),
	GeneMolecularInteraction("geneMolecularInteraction", GeneMolecularInteractionIndexer.class, IndexName.SITE_INDEX, 8, 1500, 1500, 8, 10, true),

	// This group needs parallelism
	TransgenicAllele("transgenicAlleles", TransgenicAlleleIndexer.class, IndexName.SITE_INDEX, 1, 3000, 1500, 8, 10, true),
	DiseaseAnnotation("diseaseAnnotation", DiseaseAnnotationIndexer.class, IndexName.SITE_INDEX, 1, 1500, 1500, 8, 10, true),

	GeneToGeneOrthology("geneToGeneOrthology", GeneToGeneOrthologyIndexer.class, IndexName.SITE_INDEX, 8, 2500, 2500, 8, 10, true),
	AGMAnnotation("agmAnnotation", AGMAnnotationIndexer.class, IndexName.SITE_INDEX, 8, 1500, 1500, 8, 10, true),
	GeneGeneticInteraction("geneGeneticInteraction", GeneGeneticInteractionIndexer.class, IndexName.SITE_INDEX, 8, 1500, 1500, 8, 10, true),
	GeneParalogy("geneParalogy", GeneToGeneParalogyIndexer.class, IndexName.SITE_INDEX, 8, 5000, 5000, 8, 10, true),

	Literature("literature", LiteratureIndexer.class, IndexName.SITE_INDEX, 4, 7500, 7500, 4, 10, true),
	GeneExpressionAnnotation("geneExpressionAnnotation", GeneExpressionAnnotationIndexer.class, IndexName.SITE_INDEX, 8, 125, 2000, 4, 10, true),
	SiteMapAccession("sitemap", SiteMapAccessionIndexer.class, IndexName.SITE_INDEX, 4, 1500, 1500, 8, 10, true),
	DiseaseSummary("diseaseSummary", DiseaseSummaryIndexer.class, IndexName.SITE_INDEX, 4, 1500, 1500, 4, 10, true),

	GeneSearchResult("geneSearchResult", GeneSearchResultIndexer.class, IndexName.SEARCH_RESULT, 8, 1000, 1500, 8, 10, true),
	HTPDatasetSearchResult("htpDatasetSearchResult", HTPDatasetSearchResultIndexer.class, IndexName.SEARCH_RESULT, 4, 1500, 1500, 4, 10, true),
	DiseaseSearchResult("diseaseSearchResult", DiseaseSearchResultIndexer.class, IndexName.SEARCH_RESULT, 8, 50, 50, 4, 10, true),
	GOSearchResult("goSearchResult", GOSearchResultIndexer.class, IndexName.SEARCH_RESULT, 4, 1500, 1500, 4, 10, true),
	ModelSearchResult("modelSearchResult", ModelSearchResultIndexer.class, IndexName.SEARCH_RESULT, 4, 1500, 1500, 4, 10, true),
	
	;

	private String typeName;
	private Class<?> indexClazz;
	private IndexName defaultIndexName;
	private int threadCount;
	private int bufferSize;
	private int bulkActions;
	private int concurrentRequests;
	private int bulkSize;
	private boolean runInParallel;

	IndexerConfig(String typeName, Class<?> indexClazz, IndexName defaultIndexName, int threadCount, int bufferSize, int bulkActions, int concurrentRequests, int bulkSize, boolean runInParallel) {
		this.typeName = typeName;
		this.indexClazz = indexClazz;
		this.defaultIndexName = defaultIndexName;
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
	
	public IndexName getDefaultIndexName() {
		return defaultIndexName;
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
