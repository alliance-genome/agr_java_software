package org.alliancegenome.es.model.search;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;

public class AggResult {

	public String key;
	public List<AggDocCount> values = new ArrayList<>();

	public AggResult(String key, Terms aggs) {
		this.key = key;
		for (Terms.Bucket entry : aggs.getBuckets()) {
			if (entry.getAggregations() != null && CollectionUtils.isNotEmpty(entry.getAggregations().asList())) {
				values.add(new AggDocCount(entry.getKeyAsString(),
						entry.getDocCount(),entry.getAggregations()));
			} else {
				values.add(new AggDocCount(entry.getKeyAsString(), entry.getDocCount()));
			}

		}

	}

}
