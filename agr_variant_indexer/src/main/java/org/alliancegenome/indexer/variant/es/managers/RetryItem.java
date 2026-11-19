package org.alliancegenome.indexer.variant.es.managers;

import java.util.List;

import org.alliancegenome.core.es.util.IndexName;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RetryItem {

	private final IndexName indexName;
	private List<byte[]> docs;
	private int retryCount;
	private int splitDepth;
	private String lastFailureReason;
	private final long firstFailedAt;

	public RetryItem(IndexName indexName, List<byte[]> docs) {
		this.indexName = indexName;
		this.docs = docs;
		this.retryCount = 0;
		this.splitDepth = 0;
		this.lastFailureReason = null;
		this.firstFailedAt = System.currentTimeMillis();
	}

	public RetryItem(IndexName indexName, List<byte[]> docs, int splitDepth) {
		this(indexName, docs);
		this.splitDepth = splitDepth;
	}
}
