package org.alliancegenome.indexer.util;

import java.io.IOException;

import org.alliancegenome.indexer.mapping.ESSchema;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;

public class IndexManager {

	public IndexManager() {

		try {
			XContentBuilder builder = XContentFactory.jsonBuilder().prettyPrint();
			new ESSchema(builder).buildSchemaMapping();
			String s = builder.string();
			System.out.println(s);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void createIndexes() {
		
		
	}
}
