package org.alliancegenome.indexer.variant.es.managers;

import java.util.ArrayList;
import java.util.List;

import org.alliancegenome.core.es.util.IndexName;

import lombok.Getter;

@Getter
public class IndexBatch {

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
