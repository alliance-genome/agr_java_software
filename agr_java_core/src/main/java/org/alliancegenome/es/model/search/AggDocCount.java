package org.alliancegenome.es.model.search;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;

public class AggDocCount {

    public String key;
    public long total;
    public List<AggDocCount> values = new ArrayList<>();
    
    public AggDocCount(String key, long total) {
        this.key = key;
        this.total = total;
    }

    public AggDocCount(String key, long total, Aggregations aggs) {
        this.key = key;
        this.total = total;

        //only interested in dealing with a single subAgg
        if (aggs != null && CollectionUtils.isNotEmpty(aggs.asList()) && aggs.asList().get(0) != null) {
            Terms sub = aggs.get(aggs.asList().get(0).getName());

            for (Terms.Bucket entry : sub.getBuckets()) {
                values.add(new AggDocCount(entry.getKeyAsString(),
                        entry.getDocCount(),entry.getAggregations()));
            }
        }
    }
}
