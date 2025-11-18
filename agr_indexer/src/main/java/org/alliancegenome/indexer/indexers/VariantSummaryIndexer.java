package org.alliancegenome.indexer.indexers;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.alliancegenome.api.entity.VariantSummaryDocument;
import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.curation_api.interfaces.document.VariantDocumentInterface;
import org.alliancegenome.curation_api.model.document.es.VariantSummaryDTO;
import org.alliancegenome.curation_api.response.SearchResponse;
import org.alliancegenome.es.util.ProcessDisplayHelper;
import org.alliancegenome.indexer.RestConfig;
import org.alliancegenome.indexer.config.IndexerConfig;
import si.mazi.rescu.RestProxyFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;

@Slf4j
public class VariantSummaryIndexer extends Indexer {

	private final VariantDocumentInterface transgenicAlleleApi = RestProxyFactory.createProxy(VariantDocumentInterface.class, ConfigHelper.getCurationApiUrl(), RestConfig.config);

	private final HashMap<String, Object> params = new HashMap<>() {{
		put("internal", false);
		put("obsolete", false);
	}};

	public VariantSummaryIndexer(IndexerConfig config) {
		super(config);

	}

	@Override
	protected void index() {
		SearchResponse<VariantSummaryDTO> searchResponse = transgenicAlleleApi.findDocuments(0, 0, params);
		ProcessDisplayHelper display = new ProcessDisplayHelper();
		display.startProcess("Pulling Variants from curation", searchResponse.getTotalResults());
		List<VariantSummaryDocument> list = new ArrayList<>();
		int batchSize = indexerConfig.getBufferSize();
		int maxPage = (int) (searchResponse.getTotalResults() / batchSize);
		for (int page = 0; page <= maxPage; page++) {
			SearchResponse<VariantSummaryDTO> response = transgenicAlleleApi.findDocuments(page, batchSize, params);
			for (VariantSummaryDTO da : response.getResults()) {
				if (da == null) {
					continue;
				}
				VariantSummaryDocument document = new VariantSummaryDocument();
				document.setVariant(da.getVariant());
				document.setAllele(da.getAllele());
				list.add(document);
			}
			display.progressProcess(response.getReturnedRecords().longValue());
		}
		display.finishProcess();
		indexDocuments(list);
	}

	@Override
	protected void startSingleThread(LinkedBlockingDeque<String> queue) {
	}

	@Override
	protected ObjectMapper customizeObjectMapper(ObjectMapper objectMapper) {
		return RestConfig.config.getJacksonObjectMapperFactory().createObjectMapper();
	}
}
