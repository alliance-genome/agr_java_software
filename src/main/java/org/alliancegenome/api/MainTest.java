package org.alliancegenome.api;

import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;

public class MainTest {

	public static void main(String[] args) {
		

		//System.out.println(AggregationBuilders.significantTerms("categories"));
		//term.subAggregation(AggregationBuilders.significantTerms("sig"));

		//AggregationBuilder terms = AggregationBuilders.terms("categories");
		
		
		TermsAggregationBuilder term = AggregationBuilders.terms("categories");
		term.field("category");
		term.size(999);
		
		//Map<String, Object> map = new HashMap<String, Object>();
		
		//map.put("field", "my value");
		
		//term.setMetaData(map);
		System.out.println(term);

	}

}
