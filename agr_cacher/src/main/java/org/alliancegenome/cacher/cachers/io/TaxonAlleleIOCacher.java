package org.alliancegenome.cacher.cachers.io;

import static java.util.stream.Collectors.groupingBy;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.alliancegenome.cacher.cachers.IOCacher;
import org.alliancegenome.neo4j.entity.node.Allele;

public class TaxonAlleleIOCacher extends IOCacher<Allele, List<Allele>> {

    public TaxonAlleleIOCacher(String inputCacheName, String outputCacheName) {
        super(inputCacheName, outputCacheName);
    }

    @Override
    protected void cache() {

        Map<String, List<Allele>> map = inputCache.values().stream().collect(groupingBy(allele -> allele.getSpecies().getPrimaryKey()));
        
        for(Entry<String, List<Allele>> entry: map.entrySet()) {
            outputCache.put(entry.getKey(), entry.getValue());
        }

    }

}
