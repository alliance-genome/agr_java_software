package org.alliancegenome.indexer.indexers;

import org.alliancegenome.indexer.config.IndexerConfig;
import org.alliancegenome.indexer.document.SearchableItemDocument;
import org.alliancegenome.indexer.entity.Disease;
import org.alliancegenome.indexer.entity.Gene;
import org.alliancegenome.indexer.entity.Go;
import org.alliancegenome.indexer.service.Neo4jESService;
import org.alliancegenome.indexer.translators.DiseaseToSearchableItemTranslator;
import org.alliancegenome.indexer.translators.GeneToSearchableItemTranslator;
import org.alliancegenome.indexer.translators.GoToSearchableItemTranslator;

public class SearchableItemIndexer extends Indexer<SearchableItemDocument> {

	private Neo4jESService<Gene> geneNeo4jService = new Neo4jESService<Gene>();
	private Neo4jESService<Disease> diseaseNeo4jService = new Neo4jESService<Disease>();
	private Neo4jESService<Go> goNeo4jService = new Neo4jESService<Go>();
	
	private GeneToSearchableItemTranslator geneToSI = new GeneToSearchableItemTranslator();
	private DiseaseToSearchableItemTranslator diseaseToSI = new DiseaseToSearchableItemTranslator();
	private GoToSearchableItemTranslator goToSI = new GoToSearchableItemTranslator();
	
	
	public SearchableItemIndexer(IndexerConfig config) {
		super(config);
	}
	
	@Override
	public void index() {
		
		Iterable<Gene> gene_entities = geneNeo4jService.findAll();
		Iterable<Disease> disease_entities = diseaseNeo4jService.findAll();
		Iterable<Go> go_entities = goNeo4jService.findAll();
		
		addDocuments(geneToSI.translateEntities(gene_entities));
		addDocuments(diseaseToSI.translateEntities(disease_entities));
		addDocuments(goToSI.translateEntities(go_entities));

	}

}
