package org.alliancegenome.indexer.indexers.curation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.curation_api.interfaces.document.VariantDocumentInterface;
import org.alliancegenome.curation_api.model.document.es.VariantSummaryDocument;
import org.alliancegenome.curation_api.response.SearchResponse;
import org.alliancegenome.es.rest.RestConfig;
import org.alliancegenome.indexer.config.IndexerConfig;
import org.alliancegenome.indexer.indexers.Indexer;
import org.apache.commons.collections4.CollectionUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import si.mazi.rescu.RestProxyFactory;

@Slf4j
public class VariantSummaryCurationIndexer extends Indexer {

	private final VariantDocumentInterface variantApi = RestProxyFactory.createProxy(VariantDocumentInterface.class, ConfigHelper.getCurationApiUrl(), RestConfig.config);

	private final HashMap<String, Object> params = new HashMap<>() {
		{
			put("internal", false);
			put("obsolete", false);
		}
	};

	public VariantSummaryCurationIndexer(IndexerConfig config) {
		super(config);
	}

	@Override
	protected void index() {

		try {
			SearchResponse<VariantSummaryDocument> searchResponse = variantApi.findDocuments(0, 0, params);
			log.info("VariantSummary count: " + String.format("%,d", searchResponse.getTotalResults()));

			int totalPages = (int) (searchResponse.getTotalResults() / indexerConfig.getBufferSize());

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
				SearchResponse<VariantSummaryDocument> response = variantApi.findDocuments(Integer.valueOf(page), indexerConfig.getBufferSize(), params);

				// log.info("Search Response: " + response);
				List<VariantSummaryDocument> results = response.getResults();
				if (response == null || CollectionUtils.isEmpty(results)) {
					return;
				}

				List<VariantSummaryDocument> list = new ArrayList<>();
				for (VariantSummaryDocument variantSummaryDto : results) {
					if (variantSummaryDto == null) {
						continue;
					}
					VariantSummaryDocument document = new VariantSummaryDocument();
					document.setSubCategory("LTP_variant");
					document.setVariant(variantSummaryDto.getVariant());
					document.setAllele(variantSummaryDto.getAllele());
					list.add(document);
				}

				indexDocuments(list);
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
