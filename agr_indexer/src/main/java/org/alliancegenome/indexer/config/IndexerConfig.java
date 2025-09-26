package org.alliancegenome.indexer.config;

import org.alliancegenome.indexer.indexers.AffectedGenomicModelIndexer;
import org.alliancegenome.indexer.indexers.AlleleIndexer;
import org.alliancegenome.indexer.indexers.DatasetIndexer;
import org.alliancegenome.indexer.indexers.DiseaseIndexer;
import org.alliancegenome.indexer.indexers.GeneIndexer;
import org.alliancegenome.indexer.indexers.GoIndexer;
import org.alliancegenome.indexer.indexers.LiteratureIndexer;
import org.alliancegenome.indexer.indexers.ModelIndexer;
import org.alliancegenome.indexer.indexers.TransgenicAlleleIndexer;
import org.alliancegenome.indexer.indexers.curation.AlleleSummaryCurationIndexer;
import org.alliancegenome.indexer.indexers.curation.DiseaseAnnotationCurationIndexer;
import org.alliancegenome.indexer.indexers.curation.DiseaseSummaryCurationIndexer;
import org.alliancegenome.indexer.indexers.curation.GOSearchResultCurationIndexer;
import org.alliancegenome.indexer.indexers.curation.GeneExpressionAnnotationIndexer;
import org.alliancegenome.indexer.indexers.curation.GeneGeneticInteractionCurationIndexer;
import org.alliancegenome.indexer.indexers.curation.GeneMolecularInteractionCurationIndexer;
import org.alliancegenome.indexer.indexers.curation.GeneToGeneOrthologyIndexer;
import org.alliancegenome.indexer.indexers.curation.GeneToGeneParalogyIndexer;
import org.alliancegenome.indexer.indexers.curation.PhenotypeAnnotationCurationIndexer;
import org.alliancegenome.indexer.indexers.curation.ReleaseInfoIndexer;
import org.alliancegenome.indexer.indexers.curation.SiteMapAccessionCurationIndexer;

public enum IndexerConfig {

	// Neo Indexers
	GeneIndexer("gene", GeneIndexer.class, 4, 359, 359, 8, 1, false),
	DatasetIndexer("dataset", DatasetIndexer.class, 4, 566, 566, 8, 1, false),
	DiseaseIndexer("disease", DiseaseIndexer.class, 4, 680, 680, 8, 1, false),
	AlleleIndexer("allele", AlleleIndexer.class, 4, 1517, 1517, 8, 1, false),
	GoIndexer("go", GoIndexer.class, 4, 914, 914, 8, 1, false),
	ModelIndexer("model", ModelIndexer.class, 4, 1500, 1426, 4, 1, false),

	LiteratureIndexer("literature", LiteratureIndexer.class, 4, 5000, 5000, 1, 1, true),
	
	// Curation Indexers
	DiseaseAnnotationIndexer("diseaseAnnotation", DiseaseAnnotationCurationIndexer.class, 1, 1500, 1500, 2, 1, true),
	GeneGeneticInteractionIndexers("geneGeneticInteraction", GeneGeneticInteractionCurationIndexer.class, 4, 1500, 1500, 2, 1, true),
	GeneMolecularInteractionIndexers("geneMolecularInteraction", GeneMolecularInteractionCurationIndexer.class, 4, 1500, 1500, 2, 1, true),
	ParalogyIndexer("paralogy", GeneToGeneParalogyIndexer.class, 4, 5000, 5000, 8, 1, true),
	PhenotypeAnnotationIndexer("phenotypeAnnotation", PhenotypeAnnotationCurationIndexer.class, 4, 1500, 1500, 2, 1, true),
	ReleaseInfoIndexer("release", ReleaseInfoIndexer.class, 1, 1, 1, 1, 1, true),
	DiseaseSummaryIndexer("diseaseSummary", DiseaseSummaryCurationIndexer.class, 4, 1500, 1500, 4, 1, true),
	AlleleSummaryIndexer("alleleSummary", AlleleSummaryCurationIndexer.class, 4, 500, 500, 4, 1, true),
	AffectedGenomicModelIndexer("affectedGenomicModels", AffectedGenomicModelIndexer.class, 4, 1500, 1500, 8, 1, true),
	TransgenicAlleleIndexer("transgenicAlleles", TransgenicAlleleIndexer.class, 1, 1500, 1500, 8, 1, true),
	GeneToGeneOrthologyIndexer("geneToGeneOrthology", GeneToGeneOrthologyIndexer.class, 2, 1500, 1500, 8, 1, false),
	GOSearchResultCurationIndexer("goSearchResult", GOSearchResultCurationIndexer.class, 4, 1500, 1500, 8, 1, true),
	GeneExpressionAnnotationIndexer("geneExpressionAnnotation", GeneExpressionAnnotationIndexer.class, 4, 1000, 1500, 4, 1, true),
	//GeneSearchResultCurationIndexer("geneSearchResult", GeneSearchResultCurationIndexer.class, 4, 250, 1000, 4, 1),
	
	SiteMapAccessionCurationIndexer("sitemap", SiteMapAccessionCurationIndexer.class, 4, 1500, 1500, 8, 1, true),
	
	
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
