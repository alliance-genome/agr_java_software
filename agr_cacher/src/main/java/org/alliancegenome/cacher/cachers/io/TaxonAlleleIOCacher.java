package org.alliancegenome.cacher.cachers.io;

import org.alliancegenome.cacher.cachers.IOCacher;
import org.alliancegenome.neo4j.entity.node.Allele;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.groupingBy;

public class TaxonAlleleIOCacher extends IOCacher<Allele, List<Allele>> {

    public TaxonAlleleIOCacher(String inputCacheName, String outputCacheName) {
        super(inputCacheName, outputCacheName);
    }

    @Override
    protected void cache() {
        Map<String, List<Allele>> map = inputCache.values().stream().collect(groupingBy(allele -> allele.getSpecies().getPrimaryKey()));
        map.forEach((key, value) -> outputCache.put(key, new ArrayList<>(value)));
    }

}
