package org.alliancegenome.indexer.indexers.curation;

import java.util.HashMap;
import java.util.concurrent.LinkedBlockingDeque;

import org.alliancegenome.curation_api.interfaces.document.GeneExpressionDocumentInterface;
import org.alliancegenome.curation_api.model.document.es.GeneExpressionDocument;
import org.alliancegenome.curation_api.model.entities.Gene;
import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.curation_api.response.SearchResponse;
import org.alliancegenome.indexer.RestConfig;
import org.alliancegenome.indexer.config.IndexerConfig;
import org.alliancegenome.indexer.indexers.Indexer;
import org.alliancegenome.neo4j.entity.SpeciesType;
import org.apache.commons.collections.CollectionUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import si.mazi.rescu.RestProxyFactory;

import static org.alliancegenome.neo4j.entity.SpeciesType.getPhylogeneticSortOrder;

@Slf4j
public class GeneExpressionAnnotationIndexer extends Indexer {

	private final GeneExpressionDocumentInterface geneExpressionApi = RestProxyFactory.createProxy(GeneExpressionDocumentInterface.class, ConfigHelper.getCurationApiUrl(), RestConfig.config);
	
	private HashMap<String, Object> params = new HashMap<>() {{
		put("internal", false);
		put("obsolete", false);
	}};
	
	public GeneExpressionAnnotationIndexer(IndexerConfig indexerConfig) {
		super(indexerConfig);
	}

	@Override
	protected void index() {
		try {
			SearchResponse<GeneExpressionDocument> response = geneExpressionApi.findDocument(0, 0, params);
			log.info("GeneExpressionAnnotation count: " + response.getTotalResults());
			int totalPages = (int) (response.getTotalResults() / indexerConfig.getBufferSize());
			LinkedBlockingDeque<String> queue = new LinkedBlockingDeque<>();
			for (int i = 0; i <= totalPages; i++) {
				queue.add(String.valueOf(i));
			}
			
			initiateThreading(queue);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void startSingleThread(LinkedBlockingDeque<String> queue) {
		while (true) {
			try {
				if (queue.isEmpty()) {
					return;
				}

				String page = queue.takeFirst();
				log.debug(queue.size() + " pages to process " + Thread.currentThread().getName() + " starting page: " + page);
				SearchResponse<GeneExpressionDocument> response = geneExpressionApi.findDocument(Integer.valueOf(page), indexerConfig.getBufferSize(), params);
				if (response == null || CollectionUtils.isEmpty(response.getResults())) {
					return;
				}
				for (GeneExpressionDocument ged : response.getResults()) {
					Gene gene = ged.getGeneExpressionAnnotation().getExpressionAnnotationSubject();
					if (gene != null) {
						HashMap<String, Integer> order = SpeciesType.getSpeciesOrderByTaxonID(gene.getTaxon().getCurie());
						ged.setSpeciesOrder(order);
						int phylogeneticSortOrder = getPhylogeneticSortOrder(gene.getTaxon().getCurie());
						ged.setPhylogeneticSortingIndex(phylogeneticSortOrder);
					}
				}


				indexDocuments(response.getResults());
			} catch (Exception e) {
				log.error("Error while indexing...", e);
				System.exit(-1);
				return;
			}
		}
	}

	@Override
	protected ObjectMapper customizeObjectMapper(ObjectMapper objectMapper) {
		return RestConfig.config.getJacksonObjectMapperFactory().createObjectMapper();
	}
}


