package org.alliancegenome.indexer.indexers.curation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.LinkedBlockingDeque;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.curation_api.interfaces.crud.GeneDiseaseAnnotationCrudInterface;
import org.alliancegenome.curation_api.interfaces.crud.GeneExpressionAnnotationCrudInterface;
import org.alliancegenome.curation_api.interfaces.document.GeneToGeneOrthologyDocumentInterface;
import org.alliancegenome.curation_api.model.document.es.GeneToGeneOrthologyDocument;
import org.alliancegenome.curation_api.response.SearchResponse;
import org.alliancegenome.es.rest.RestConfig;
import org.alliancegenome.indexer.config.IndexerConfig;
import org.alliancegenome.indexer.indexers.Indexer;
import org.alliancegenome.indexer.indexers.curation.service.BaseService;
import org.apache.commons.collections4.CollectionUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import si.mazi.rescu.RestProxyFactory;

@Slf4j
public class GeneToGeneOrthologyIndexer extends Indexer {

	private final GeneToGeneOrthologyDocumentInterface orthologyApi = RestProxyFactory.createProxy(GeneToGeneOrthologyDocumentInterface.class, ConfigHelper.getCurationApiUrl(), RestConfig.config);
	private final GeneExpressionAnnotationCrudInterface geneExpressionApi = RestProxyFactory.createProxy(GeneExpressionAnnotationCrudInterface.class, ConfigHelper.getCurationApiUrl(), RestConfig.config);
	private final GeneDiseaseAnnotationCrudInterface geneDiseaseApi = RestProxyFactory.createProxy(GeneDiseaseAnnotationCrudInterface.class, ConfigHelper.getCurationApiUrl(), RestConfig.config);

	private Set<String> allNeoGeneIDs;
	private Set<String> geneExpressionSet;
	private Set<String> geneAnnotationSet;

	private List<List<Long>> idBatches;

	public GeneToGeneOrthologyIndexer(IndexerConfig config) {
		super(config);
	}

	@Override
	public void index() {
		BaseService baseService = new BaseService();
		allNeoGeneIDs = baseService.getAllNeoGeneIDs();
		geneExpressionSet = new HashSet<>(geneExpressionApi.annotatedGeneList().getEntities());
		geneAnnotationSet = new HashSet<>(geneDiseaseApi.annotatedGeneList().getEntities());

		try {
			log.info("Fetching all orthology IDs...");
			SearchResponse<Long> idsResponse = orthologyApi.getAllIds();
			List<Long> allIds = idsResponse.getResults();
			log.info("Fetched {} orthology IDs", allIds.size());

			idBatches = partition(allIds, indexerConfig.getBufferSize());
			log.info("Partitioned into {} batches of up to {}", idBatches.size(), indexerConfig.getBufferSize());

			LinkedBlockingDeque<String> queue = new LinkedBlockingDeque<>();
			for (int i = 0; i < idBatches.size(); i++) {
				queue.add(String.valueOf(i));
			}

			initiateThreading(queue);
		} catch (Exception e) {
			log.error("Error while indexing...", e);
			System.exit(-1);
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
				List<Long> batchIds = idBatches.get(Integer.parseInt(batchIndex));

				SearchResponse<GeneToGeneOrthologyDocument> response = orthologyApi.findByIds(batchIds);
				if (response == null || CollectionUtils.isEmpty(response.getResults())) {
					continue;
				}

				List<GeneToGeneOrthologyDocument> results = response.getResults();
				for (GeneToGeneOrthologyDocument geneToGeneOrthologyDocument : results) {
					List<Map<String, Object>> geneAnnotations = geneToGeneOrthologyDocument.getGeneAnnotations();

					for (Map<String, Object> geneAnnotation : geneAnnotations) {
						String primaryExternalId = (String) geneAnnotation.get("geneIdentifier");
						geneAnnotation.put("hasExpressionAnnotations", geneExpressionSet.contains(primaryExternalId));
						geneAnnotation.put("hasDiseaseAnnotations", geneAnnotationSet.contains(primaryExternalId));
					}
				}

				List<GeneToGeneOrthologyDocument> filteredResults = filterValidResults(results);
				indexDocuments(filteredResults);
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

	private List<GeneToGeneOrthologyDocument> filterValidResults(List<GeneToGeneOrthologyDocument> docs) {
		List<GeneToGeneOrthologyDocument> result = new ArrayList<>();
		for (GeneToGeneOrthologyDocument doc : docs) {
			String curie = doc.getGeneToGeneOrthologyGenerated().getObjectGene().getIdentifier();
			if (allNeoGeneIDs.contains(curie)) {
				result.add(doc);
			}
		}
		return result;
	}
}
