package org.alliancegenome.indexer.indexers.curation;

import java.util.ArrayList;
import java.util.HashMap;
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
import org.alliancegenome.indexer.RestConfig;
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

	private HashMap<String, Object> params = new HashMap<>() {
		{
			put("internal", false);
			put("obsolete", false);
		}
	};

	public GeneToGeneOrthologyIndexer(IndexerConfig config) {
		super(config);
	}

	@Override
	public void index() {
		BaseService baseService = new BaseService();
		allNeoGeneIDs = baseService.getAllNeoGeneIDs();
		geneExpressionSet = buildGeneExpressionSet();
		geneAnnotationSet = buildGeneAnnotationSet();

		try {
			SearchResponse<GeneToGeneOrthologyDocument> resp = orthologyApi.findDocument(0, 0, params);
			log.info("GeneToGeneOrthology count: " + String.format("%,d", resp.getTotalResults()));
			int totalPages = (int) (resp.getTotalResults() / indexerConfig.getBufferSize());

			LinkedBlockingDeque<String> queue = new LinkedBlockingDeque<>();
			for (int i = 0; i <= totalPages; i++) {
				queue.add(String.valueOf(i));
			}
			initiateThreading(queue);
		} catch (Exception e) {
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
				// log.info(queue.size() + " pages to process " +
				// Thread.currentThread().getName() + " starting page: " + page);

				SearchResponse<GeneToGeneOrthologyDocument> response = orthologyApi.findDocument(Integer.valueOf(page), indexerConfig.getBufferSize(), params);
				// log.info("Search Response: " + response);
				List<GeneToGeneOrthologyDocument> results = response.getResults();
				if (response == null || CollectionUtils.isEmpty(results)) {
					return;
				}
				for (GeneToGeneOrthologyDocument geneToGeneOrthologyDocument : results) {
					List<Map<String, Object>> geneAnnotations = geneToGeneOrthologyDocument.getGeneAnnotations();

					for (Map<String, Object> geneAnnotation : geneAnnotations) {
						String primaryExternalId = (String) geneAnnotation.get("geneIdentifier");
						geneAnnotation.put("hasExpressionAnnotations", geneExpressionSet.contains(primaryExternalId));
						geneAnnotation.put("hasDiseaseAnnotations", geneAnnotationSet.contains(primaryExternalId));

					}
				}

				List<GeneToGeneOrthologyDocument> filteredResults = filterValidResults(response.getResults());
				indexDocuments(filteredResults);
			} catch (Exception e) {
				log.error("Error while indexing...", e);
				System.exit(-1);
				return;
			}
		}
	}

	private Set<String> buildGeneExpressionSet() {
		return geneExpressionApi.geneExpressionAnnotationMap().getEntity();
	}

	private Set<String> buildGeneAnnotationSet() {
		return geneDiseaseApi.geneDiseaseAnnotationMap().getEntity();
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
