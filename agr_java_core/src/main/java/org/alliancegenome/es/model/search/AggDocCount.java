package org.alliancegenome.es.model.search;

public class AggDocCount {

    public String key;
    public long total;
    
    public AggDocCount(String key, long total) {
        this.key = key;
        this.total = total;
    }
}
