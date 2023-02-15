package org.alliancegenome.indexer.processors;

import org.alliancegenome.indexer.config.IndexerConfig;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.client.RestHighLevelClient;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ESDocumentProcessor {

	public static String indexName;
	protected IndexerConfig indexerConfig;
	private RestHighLevelClient searchClient;
	protected BulkProcessor bulkProcessor;
	

	public ESDocumentProcessor() {

//		searchClient = EsClientFactory.getDefaultEsClient();
//
//		BulkProcessor.Listener listener = new BulkProcessor.Listener() {
//			@Override
//			public void beforeBulk(long executionId, BulkRequest request) {
//			}
//
//			@Override
//			public void afterBulk(long executionId, BulkRequest request, BulkResponse response) {
//				//log.info("Size: " + request.requests().size() + " MB: " + request.estimatedSizeInBytes() + " Time: " + response.getTook() + " Bulk Requet Finished");
//			}
//
//			@Override
//			public void afterBulk(long executionId, BulkRequest request, Throwable failure) {
//				log.error("Bulk Request Failure: " + failure.getMessage());
//				for(DocWriteRequest<?> req: request.requests()) {
//					IndexRequest idxreq = (IndexRequest)req;
//					bulkProcessor.add(idxreq);
//				}
//				log.error("Finished Adding failed requests to bulkProcessor: ");
//			}
//		};
//
//		BulkProcessor.Builder builder = BulkProcessor.builder((request, bulkListener) -> searchClient.bulkAsync(request, RequestOptions.DEFAULT, bulkListener), listener, getClass().getSimpleName());
//		builder.setBulkActions(indexerConfig.getBulkActions());
//		builder.setBulkSize(new ByteSizeValue(indexerConfig.getBulkSize(), ByteSizeUnit.MB));
//		builder.setConcurrentRequests(indexerConfig.getConcurrentRequests());
//		builder.setBackoffPolicy(BackoffPolicy.exponentialBackoff(TimeValue.timeValueSeconds(10L), 60));
//
//		bulkProcessor = builder.build();
	}
}
