package org.alliancegenome.indexer.variant.es.managers;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.ConnectionClosedException;
import org.apache.http.conn.ConnectTimeoutException;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.xcontent.XContentType;

public class BulkSubmitter {

	private BulkSubmitter() { }

	public static BulkSubmitResult submitOnce(List<byte[]> docs, RestHighLevelClient client, String routing, String indexName) {
		BulkRequest bulkRequest = new BulkRequest().timeout(TimeValue.timeValueHours(1));
		for (byte[] smileDoc : docs) {
			bulkRequest.add(new IndexRequest(indexName).source(smileDoc, XContentType.SMILE).routing(routing));
		}

		try {
			BulkResponse response = client.bulk(bulkRequest, RequestOptions.DEFAULT);

			if (response.hasFailures()) {
				List<byte[]> failedDocs = new ArrayList<>();
				String firstFailureMessage = null;
				for (BulkItemResponse item : response) {
					if (item.isFailed()) {
						failedDocs.add(docs.get(item.getItemId()));
						if (firstFailureMessage == null) {
							firstFailureMessage = item.getFailureMessage();
						}
					}
				}
				if (!failedDocs.isEmpty()) {
					return BulkSubmitResult.partialFailure(failedDocs, firstFailureMessage);
				}
			}
			return BulkSubmitResult.success();

		} catch (ElasticsearchStatusException e) {
			return BulkSubmitResult.fullFailure("HTTP " + e.status().getStatus() + ": " + e.getMessage(), false, true);
		} catch (ConnectTimeoutException e) {
			return BulkSubmitResult.fullFailure("ConnectTimeoutException: " + e.getMessage(), true, false);
		} catch (SocketTimeoutException e) {
			return BulkSubmitResult.fullFailure("SocketTimeoutException: " + e.getMessage(), true, false);
		} catch (ConnectionClosedException e) {
			return BulkSubmitResult.fullFailure("ConnectionClosedException: " + e.getMessage(), true, false);
		} catch (ConnectException e) {
			return BulkSubmitResult.fullFailure("ConnectException: " + e.getMessage(), true, false);
		} catch (IOException e) {
			return BulkSubmitResult.fullFailure("IOException: " + e.getMessage(), true, false);
		} catch (RuntimeException e) {
			return BulkSubmitResult.fullFailure("RuntimeException (" + e.getClass().getSimpleName() + "): " + e.getMessage(), true, false);
		}
	}
}
