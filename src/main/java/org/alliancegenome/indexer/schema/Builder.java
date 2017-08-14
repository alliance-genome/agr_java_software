package org.alliancegenome.indexer.schema;

import java.io.IOException;

import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;

public class Builder {

	protected XContentBuilder builder;

	public Builder(Boolean pretty) {
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
	
	public void init(boolean pretty) {
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
}
