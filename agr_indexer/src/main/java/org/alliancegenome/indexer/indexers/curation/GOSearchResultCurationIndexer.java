package org.alliancegenome.indexer.indexers.curation;

import java.util.HashMap;
import java.util.concurrent.LinkedBlockingDeque;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.curation_api.interfaces.document.GODocumentInterface;
import org.alliancegenome.curation_api.model.document.es.GOSearchResultDocument;
import org.alliancegenome.curation_api.response.SearchResponse;
import org.alliancegenome.indexer.RestConfig;
import org.alliancegenome.indexer.config.IndexerConfig;
import org.alliancegenome.indexer.indexers.Indexer;

import lombok.extern.slf4j.Slf4j;
import si.mazi.rescu.RestProxyFactory;

@Slf4j
public class GOSearchResultCurationIndexer extends Indexer {

    private final GODocumentInterface goApi = RestProxyFactory.createProxy(GODocumentInterface.class, ConfigHelper.getCurationApiUrl(), RestConfig.config);

    private HashMap<String, Object> params = new HashMap<String, Object>() {{
        put("internal", false);
        put("obsolete", false);
    }};

    public GOSearchResultCurationIndexer(IndexerConfig indexerConfig) {
        super(indexerConfig);
    }

    @Override
    protected void index() {
        try {
            Thread.sleep(3000); // Give ES a moment to initialize
            SearchResponse<GOSearchResultDocument> response = goApi.findSearchResult(0, 1000, params);
            log.info("Indexing {} GO terms", response.getResults().size());
            indexDocuments(response.getResults());
        } catch (Exception e) {
            log.error("Error while indexing GO terms", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void startSingleThread(LinkedBlockingDeque<String> queue) {
        // Not needed for basic indexing
    }

    @Override
    protected ObjectMapper customizeObjectMapper(ObjectMapper objectMapper) {
        return RestConfig.config.getJacksonObjectMapperFactory().createObjectMapper();
    }
}