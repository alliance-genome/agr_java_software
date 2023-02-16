package org.alliancegenome.indexer.processors;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.alliancegenome.es.util.EsClientFactory;
import org.alliancegenome.indexer.config.IndexerConfig;
import org.elasticsearch.action.DocWriteRequest;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RestHighLevelClient;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ESDocumentProcessor {

	private String indexName;
	private IndexerConfig indexerConfig;
	private RestHighLevelClient searchClient;
	private BulkProcessor bulkProcessor;

	public ESDocumentProcessor(String indexName) {
		this.indexName = indexName;
	}

	public void readFiles() {

		searchClient = EsClientFactory.getMustCloseSearchClient();

		BulkProcessor.Listener listener = new BulkProcessor.Listener() {
			@Override
			public void beforeBulk(long executionId, BulkRequest request) {
			}

			@Override
			public void afterBulk(long executionId, BulkRequest request, BulkResponse response) {
				//log.info("Size: " + request.requests().size() + " MB: " + request.estimatedSizeInBytes() + " Time: " + response.getTook() + " Bulk Requet Finished");
			}

			@Override
			public void afterBulk(long executionId, BulkRequest request, Throwable failure) {
				log.error("Bulk Request Failure: " + failure.getMessage());
				for(DocWriteRequest<?> req: request.requests()) {
					IndexRequest idxreq = (IndexRequest)req;
					bulkProcessor.add(idxreq);
				}
				log.error("Finished Adding failed requests to bulkProcessor: ");
			}
		};

//		BulkProcessor.Builder builder = BulkProcessor.builder((request, bulkListener) -> searchClient.bulkAsync(request, RequestOptions.DEFAULT, bulkListener), listener, getClass().getSimpleName());
//		builder.setBulkActions(indexerConfig.getBulkActions());
//		builder.setBulkSize(new ByteSizeValue(indexerConfig.getBulkSize(), ByteSizeUnit.MB));
//		builder.setConcurrentRequests(indexerConfig.getConcurrentRequests());
//		builder.setBackoffPolicy(BackoffPolicy.exponentialBackoff(TimeValue.timeValueSeconds(10L), 60));
//
//		bulkProcessor = builder.build();
		
		
		
	}


	public void processIndexes() {
		
		for(IndexerConfig config: IndexerConfig.values()) {
			
			try {
				BufferedReader reader = new BufferedReader(new FileReader(new File("/data/" + config.getIndexClazz().getSimpleName() + "_data.json")));
				reader.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			
			log.info("Indexer: " + config.getIndexClazz().getSimpleName());
		}
		
		
	}

	public void close() {
		try {
			searchClient.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
