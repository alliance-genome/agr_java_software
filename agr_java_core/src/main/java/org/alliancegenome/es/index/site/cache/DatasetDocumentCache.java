package org.alliancegenome.es.index.site.cache;

import java.util.*;

import org.alliancegenome.es.index.site.document.SearchableItemDocument;
import org.alliancegenome.neo4j.entity.node.HTPDataset;

import lombok.*;

@Getter
@Setter
public class DatasetDocumentCache extends IndexerCache {

    private Map<String, HTPDataset> datasetMap = new HashMap<>();

    @Override
    protected <D extends SearchableItemDocument> void addExtraCachedFields(D document) {
        // TODO Auto-generated method stub
        
    }

}
