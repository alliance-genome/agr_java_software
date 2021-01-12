package org.alliancegenome.es.index.site.cache;

import java.util.HashMap;
import java.util.Map;

import org.alliancegenome.neo4j.entity.node.HTPDataset;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DatasetDocumentCache extends IndexerCache {

    private Map<String, HTPDataset> datasetMap = new HashMap<>();

}
