package org.alliancegenome.indexer.variant.es.managers;

import java.util.ArrayList;
import java.util.List;

import org.alliancegenome.core.es.util.IndexName;

import lombok.Getter;

@Getter
public class IndexBatch {

	// Queued once per RoutedBulkIndexer behind the last real batch: flush, close the client and stop
	public static final IndexBatch END = new IndexBatch(null);

	private final IndexName indexName;
	private final List<byte[]> docs = new ArrayList<>();
	private long bytes;

	public IndexBatch(IndexName indexName) {
		this.indexName = indexName;
	}

	public void add(byte[] doc) {
		docs.add(doc);
		bytes += doc.length;
	}
}
