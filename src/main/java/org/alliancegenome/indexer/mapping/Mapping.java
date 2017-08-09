package org.alliancegenome.indexer.mapping;

import java.io.IOException;

import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;

public abstract class Mapping {

	protected XContentBuilder builder;

	public Mapping(Boolean pretty) {
		try {
			if(pretty) {
				builder = XContentFactory.jsonBuilder().prettyPrint();
			} else {
				builder = XContentFactory.jsonBuilder();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public XContentBuilder getBuilder() {
		return builder;
	}

	public abstract String buildMapping();
	
}
