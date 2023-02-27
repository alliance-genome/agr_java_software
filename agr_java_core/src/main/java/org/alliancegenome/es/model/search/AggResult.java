package org.alliancegenome.es.model.search;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;

import lombok.Data;
import lombok.ToString;

@Data @ToString
public class AggResult {

	private String key;
	private List<AggDocCount> values = new ArrayList<>();

	public AggResult(String key, Terms aggs, Set<String> acceptableKeys) {
		this.key = key;
		for (Terms.Bucket entry : aggs.getBuckets()) {
			if(acceptableKeys == null || acceptableKeys.contains(entry.getKeyAsString())) {
				if (entry.getAggregations() != null && CollectionUtils.isNotEmpty(entry.getAggregations().asList())) {
					values.add(new AggDocCount(entry.getKeyAsString(), entry.getDocCount(), entry.getAggregations()));
				} else {
					values.add(new AggDocCount(entry.getKeyAsString(), entry.getDocCount()));
				}
			} else {
				//Log.info("Key not found in acceptable keys: " + entry.getKeyAsString());
			}
		}
	}
}
