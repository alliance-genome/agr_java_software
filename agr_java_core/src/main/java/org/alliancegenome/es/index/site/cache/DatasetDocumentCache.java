package org.alliancegenome.es.index.site.cache;

import lombok.Getter;
import lombok.Setter;
import org.alliancegenome.neo4j.entity.node.HTPDataset;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class DatasetDocumentCache extends IndexerCache {

    private Map<String, HTPDataset> datasetMap = new HashMap<>();

}
