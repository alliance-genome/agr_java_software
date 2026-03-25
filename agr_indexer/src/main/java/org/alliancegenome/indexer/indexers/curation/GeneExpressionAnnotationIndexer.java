package org.alliancegenome.indexer.indexers.curation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingDeque;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.curation_api.interfaces.document.GeneExpressionDocumentInterface;
import org.alliancegenome.curation_api.model.document.es.GeneExpressionDocument;
import org.alliancegenome.curation_api.model.entities.Species;
import org.alliancegenome.curation_api.response.SearchResponse;
import org.alliancegenome.es.rest.RestConfig;
import org.alliancegenome.exceptional.client.ExceptionCatcher;
import org.alliancegenome.indexer.config.IndexerConfig;
import org.alliancegenome.indexer.indexers.Indexer;
import org.alliancegenome.indexer.indexers.curation.interfaces.SpeciesInterface;
import org.apache.commons.collections.CollectionUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import si.mazi.rescu.RestProxyFactory;

@Slf4j
public class GeneExpressionAnnotationIndexer extends Indexer {

	private final GeneExpressionDocumentInterface geneExpressionApi = RestProxyFactory.createProxy(GeneExpressionDocumentInterface.class, ConfigHelper.getCurationApiUrl(), RestConfig.config);
	private final SpeciesInterface speciesApi = RestProxyFactory.createProxy(SpeciesInterface.class, ConfigHelper.getCurationApiUrl(), RestConfig.config);

	private List<List<String>> idBatches;

	// taxonIdPart (e.g. "9606") -> phylogeneticOrder
	private Map<String, Integer> speciesOrderLookup;

	public GeneExpressionAnnotationIndexer(IndexerConfig indexerConfig) {
		super(indexerConfig);
	}

	@Override
	protected void index() {
		try {
			speciesOrderLookup = new HashMap<>();
			List<Species> allSpecies = speciesApi.findForPublic(0, 100, "FieldsOnly", new HashMap<>()).getResults();
			for (Species species : allSpecies) {
				if (species.getTaxon() != null && species.getPhylogeneticOrder() != null) {
					String taxonIdPart = species.getTaxon().getCurie().replace("NCBITaxon:", "");
					speciesOrderLookup.put(taxonIdPart, species.getPhylogeneticOrder());
				}
			}
			log.info("Loaded {} species for speciesOrder lookup", speciesOrderLookup.size());

			log.info("Fetching all gene IDs...");
			SearchResponse<String> idsResponse = geneExpressionApi.getGeneIds();

			List<String> primaryExternalIds = idsResponse.getResults();
			log.info("Fetched {} gene IDs", primaryExternalIds.size());

			idBatches = partition(primaryExternalIds, indexerConfig.getBufferSize());
			log.info("Partitioned into {} batches of up to {}", idBatches.size(), indexerConfig.getBufferSize());

			LinkedBlockingDeque<String> queue = new LinkedBlockingDeque<>();
			for (int i = 0; i < idBatches.size(); i++) {
				queue.add(String.valueOf(i));
			}

			initiateThreading(queue);
		} catch (InterruptedException e) {
			ExceptionCatcher.report(e);
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
				String batchIndex = queue.takeFirst();
				List<String> batchIds = idBatches.get(Integer.parseInt(batchIndex));
				SearchResponse<GeneExpressionDocument> response = geneExpressionApi.getConsolidateDocumentsForGenes(batchIds);

				if (response == null || CollectionUtils.isEmpty(response.getResults())) {
					continue;
				}

				for (GeneExpressionDocument doc : response.getResults()) {
					if (doc.getGeneExpressionAnnotation() != null && doc.getGeneExpressionAnnotation().getExpressionAnnotationSubject() != null && doc.getGeneExpressionAnnotation().getExpressionAnnotationSubject().getTaxon() != null) {
						doc.setSpeciesOrder(buildSpeciesOrder(doc.getGeneExpressionAnnotation().getExpressionAnnotationSubject().getTaxon().getCurie()));
					}
				}

				indexDocuments(response.getResults());
			} catch (Exception e) {
				log.error("Error while indexing...", e);
				ExceptionCatcher.report(e);
				System.exit(-1);
				return;
			}
		}
	}

	private HashMap<String, Integer> buildSpeciesOrder(String taxonCurie) {
		HashMap<String, Integer> order = new HashMap<>();
		String subjectTaxonIdPart = taxonCurie.replace("NCBITaxon:", "");
		for (Map.Entry<String, Integer> entry : speciesOrderLookup.entrySet()) {
			order.put(entry.getKey(), entry.getValue());
		}
		order.put(subjectTaxonIdPart, 0);
		return order;
	}

	@Override
	protected ObjectMapper customizeObjectMapper(ObjectMapper objectMapper) {
		return RestConfig.config.getJacksonObjectMapperFactory().createObjectMapper();
	}
}
