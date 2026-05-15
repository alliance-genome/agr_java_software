package org.alliancegenome.indexer.indexers;

import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.core.util.ListUtils;
import org.alliancegenome.core.variant.converter.AlleleSearchResultConverter;
import org.alliancegenome.core.variant.converter.AlleleSequenceSummaryConverter;
import org.alliancegenome.core.variant.converter.AlleleToVariantSummaryConverter;
import org.alliancegenome.curation_api.interfaces.document.AlleleDocumentInterface;
import org.alliancegenome.curation_api.model.document.es.AlleleSummaryDocument;
import org.alliancegenome.curation_api.model.document.es.SequenceSummaryDocument;
import org.alliancegenome.curation_api.model.document.es.VariantSummaryDocument;
import org.alliancegenome.curation_api.response.SearchResponse;
import org.alliancegenome.curation_api.view.CurationView;
import org.alliancegenome.core.document.AlleleSearchResultDocument;
import org.alliancegenome.core.config.RestConfig;
import org.alliancegenome.core.es.util.ProcessDisplayHelper;
import org.alliancegenome.exceptional.client.ExceptionCatcher;
import org.alliancegenome.indexer.config.IndexerConfig;
import org.apache.commons.collections.CollectionUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import si.mazi.rescu.RestProxyFactory;

@Slf4j
public class AlleleSummaryCurationIndexer extends Indexer {

	private final AlleleDocumentInterface alleleApi = RestProxyFactory.createProxy(AlleleDocumentInterface.class, ConfigHelper.getCurationApiUrl(), RestConfig.config);
	private final AlleleSequenceSummaryConverter sequenceSummaryConverter = new AlleleSequenceSummaryConverter();
	private final AlleleSearchResultConverter alleleSearchResultConverter = new AlleleSearchResultConverter();
	private final AlleleToVariantSummaryConverter variantSummaryConverter = new AlleleToVariantSummaryConverter();

	private List<List<Long>> idBatches;

	public AlleleSummaryCurationIndexer(IndexerConfig indexerConfig) {
		super(indexerConfig);
	}

	@Override
	protected void index(ProcessDisplayHelper display) {
		try {
			log.info("Fetching all allele IDs...");
			SearchResponse<Long> idsResponse = alleleApi.getAllIds();
			List<Long> allIds = idsResponse.getResults();
			log.info("Fetched {} allele IDs", allIds.size());
			display.startProcess(allIds.size());
			idBatches = ListUtils.partition(allIds, indexerConfig.getBufferSize());
			log.info("Partitioned into {} batches of up to {}", idBatches.size(), indexerConfig.getBufferSize());

			LinkedBlockingDeque<String> queue = new LinkedBlockingDeque<>();
			for (int i = 0; i < idBatches.size(); i++) {
				queue.add(String.valueOf(i));
			}

			initiateThreading(queue);
		} catch (Exception e) {
			log.error("Error while indexing...", e);
			ExceptionCatcher.report(e);
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

				SearchResponse<AlleleSummaryDocument> response = alleleApi.findSummaryByIds(batchIds);
				if (response == null || CollectionUtils.isEmpty(response.getResults())) {
					continue;
				}

				// Convert to derived documents first (consumes transport-only fields)
				List<SequenceSummaryDocument> sequenceDocs = sequenceSummaryConverter.convert(response.getResults());
				List<AlleleSearchResultDocument> searchDocs = alleleSearchResultConverter.convert(response.getResults());
				List<VariantSummaryDocument> variantDocs = variantSummaryConverter.convert(response.getResults());

				// Strip fields only needed by derived documents before indexing to ES
				response.getResults().forEach(AlleleSummaryDocument::removeTransportFields);

				// Index all document types
				indexDocuments(response.getResults());
				indexDocuments(sequenceDocs, CurationView.SequenceSummaryDocument.class);
				indexDocuments(searchDocs);
				indexDocuments(variantDocs, CurationView.VariantSummaryDocument.class);
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