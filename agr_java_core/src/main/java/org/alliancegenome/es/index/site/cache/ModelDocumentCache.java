package org.alliancegenome.es.index.site.cache;

import java.util.*;

import org.alliancegenome.es.index.site.document.SearchableItemDocument;
import org.alliancegenome.neo4j.entity.node.AffectedGenomicModel;

import lombok.*;

@Getter
@Setter
public class ModelDocumentCache extends IndexerCache {

    private Map<String, AffectedGenomicModel> modelMap = new HashMap<>();

    @Override
    protected <D extends SearchableItemDocument> void addExtraCachedFields(D document) {

    }

}
