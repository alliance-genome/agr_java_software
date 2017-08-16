package org.alliancegenome.indexer.indexers;

import org.alliancegenome.indexer.config.IndexerConfig;
import org.alliancegenome.indexer.document.searchableitem.SearchableItemDocument;
import org.alliancegenome.indexer.entity.DOTerm;
import org.alliancegenome.indexer.entity.Gene;
import org.alliancegenome.indexer.entity.GOTerm;
import org.alliancegenome.indexer.service.Neo4jESService;
import org.alliancegenome.indexer.translators.DiseaseToSearchableItemTranslator;
import org.alliancegenome.indexer.translators.GeneToSearchableItemTranslator;
import org.alliancegenome.indexer.translators.GoToSearchableItemTranslator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SearchableItemIndexer extends Indexer<SearchableItemDocument> {

	private Neo4jESService<Gene> geneNeo4jService = new Neo4jESService<Gene>(Gene.class);
	private Neo4jESService<DOTerm> diseaseNeo4jService = new Neo4jESService<DOTerm>(DOTerm.class);
	private Neo4jESService<GOTerm> goNeo4jService = new Neo4jESService<GOTerm>(GOTerm.class);

	private GeneToSearchableItemTranslator geneToSI = new GeneToSearchableItemTranslator();
	private DiseaseToSearchableItemTranslator diseaseToSI = new DiseaseToSearchableItemTranslator();
	private GoToSearchableItemTranslator goToSI = new GoToSearchableItemTranslator();

	private Logger log = LogManager.getLogger(getClass());


	public SearchableItemIndexer(IndexerConfig indexConfig) {
		super(indexConfig);
	}

	@Override
	public void index() {

		int geneCount = geneNeo4jService.getCount();
		int chunkSize = 1000;
		int pages = geneCount / chunkSize;

		log.debug("GeneCount: " + geneCount);
		if(geneCount > 0) {
			startProcess(pages, chunkSize, geneCount);
			for(int i = 0; i <= pages; i++) {
				Iterable<Gene> gene_entities = geneNeo4jService.getPage(i, chunkSize);
				addDocuments(geneToSI.translateEntities(gene_entities));
				progress(i, pages, chunkSize);
			}
			finishProcess(geneCount);
		}

		int diseaseCount = diseaseNeo4jService.getCount();
		pages = diseaseCount / chunkSize;

		log.debug("DiseaseCount: " + diseaseCount);
		
		if(diseaseCount > 0) {
			startProcess(pages, chunkSize, diseaseCount);
			for(int i = 0; i <= pages; i++) {
				Iterable<DOTerm> disease_entities = diseaseNeo4jService.getPage(i, chunkSize);
				addDocuments(diseaseToSI.translateEntities(disease_entities));
				progress(i, pages, chunkSize);
			}
			finishProcess(diseaseCount);
		}
		
		int goCount = goNeo4jService.getCount();
		chunkSize = 500;
		pages = goCount / chunkSize;

		log.debug("GoCount: " + goCount);
		if(goCount > 0) {
			startProcess(pages, chunkSize, goCount);
			for(int i = 0; i <= pages; i++) {
				Iterable<GOTerm> go_entities = goNeo4jService.getPage(i, chunkSize);
				log.info(go_entities);
				addDocuments(goToSI.translateEntities(go_entities));
				progress(i, pages, chunkSize);
			}
			finishProcess(goCount);
		}

	}

}
