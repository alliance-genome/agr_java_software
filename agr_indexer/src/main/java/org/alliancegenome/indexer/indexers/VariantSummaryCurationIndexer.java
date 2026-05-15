package org.alliancegenome.indexer.indexers;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.core.variant.converters.VariantSearchResultConverter;
import org.alliancegenome.curation_api.interfaces.document.VariantDocumentInterface;
import org.alliancegenome.curation_api.model.document.es.VariantSummaryDocument;
import org.alliancegenome.curation_api.response.SearchResponse;
import org.alliancegenome.curation_api.view.CurationView;
import org.alliancegenome.es.model.VariantSearchResultDocument;
import org.alliancegenome.es.rest.RestConfig;
import org.alliancegenome.es.util.ProcessDisplayHelper;
import org.alliancegenome.exceptional.client.ExceptionCatcher;
import org.alliancegenome.indexer.config.IndexerConfig;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import si.mazi.rescu.RestProxyFactory;

@Slf4j
public class VariantSummaryCurationIndexer extends Indexer {

	private final VariantDocumentInterface variantApi = RestProxyFactory.createProxy(VariantDocumentInterface.class, ConfigHelper.getCurationApiUrl(), RestConfig.config);

	private VariantSearchResultConverter variantSearchResultConverter = new VariantSearchResultConverter();
	
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
	protected void index(ProcessDisplayHelper display) {

		try {
			SearchResponse<VariantSummaryDocument> searchResponse = variantApi.findDocuments(0, 0, params);
			log.info("VariantSummary count: " + String.format("%,d", searchResponse.getTotalResults()));
			display.startProcess(searchResponse.getTotalResults());
			int totalPages = (int) (searchResponse.getTotalResults() / indexerConfig.getBufferSize());

			LinkedBlockingDeque<String> queue = new LinkedBlockingDeque<>();
			for (int i = 0; i <= totalPages; i++) {
				queue.add(String.valueOf(i));
			}
			initiateThreading(queue);
		} catch (Exception e) {
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

				String page = queue.takeFirst();
				
				SearchResponse<VariantSummaryDocument> response = variantApi.findDocuments(Integer.valueOf(page), indexerConfig.getBufferSize(), params);
				indexDocuments(response.getResults(), CurationView.VariantSummaryDocument.class);
				
				List<VariantSearchResultDocument> vsrd = variantSearchResultConverter.convertToVariantSearchDocument(response.getResults());
				indexDocuments(vsrd);
				
			} catch (Exception e) {
				log.error("Error while indexing...", e);
				ExceptionCatcher.report(e);
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
