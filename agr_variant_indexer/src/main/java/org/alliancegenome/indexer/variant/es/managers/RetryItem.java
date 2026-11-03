package org.alliancegenome.indexer.variant.es.managers;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RetryItem {

	private List<byte[]> docs;
	private int retryCount;
	private int splitDepth;
	private String lastFailureReason;
	private final long firstFailedAt;

	public RetryItem(List<byte[]> docs) {
		this.docs = docs;
		this.retryCount = 0;
		this.splitDepth = 0;
		this.lastFailureReason = null;
		this.firstFailedAt = System.currentTimeMillis();
	}

	public RetryItem(List<byte[]> docs, int splitDepth) {
		this(docs);
		this.splitDepth = splitDepth;
	}
}
