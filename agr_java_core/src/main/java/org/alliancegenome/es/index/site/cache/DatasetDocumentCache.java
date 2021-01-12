package org.alliancegenome.es.index.site.cache;

import java.util.*;

import org.alliancegenome.neo4j.entity.node.HTPDataset;

import lombok.*;

@Getter
@Setter
public class DatasetDocumentCache extends IndexerCache {

    private Map<String, HTPDataset> datasetMap = new HashMap<>();

}
