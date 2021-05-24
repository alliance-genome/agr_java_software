package org.alliancegenome.es.index.site.cache;

import java.util.*;

import org.alliancegenome.es.index.site.document.SearchableItemDocument;

import lombok.*;

@Getter
@Setter
public class AlleleDocumentCache extends IndexerCache {

    protected Map<String, Set<String>> constructExpressedComponents = new HashMap<>();
    protected Map<String, Set<String>> constructKnockdownComponents = new HashMap<>();
    protected Map<String, Set<String>> constructRegulatoryRegions = new HashMap<>();


    @Override
    protected <D extends SearchableItemDocument> void addExtraCachedFields(D document) {
        String id = document.getPrimaryKey();
        document.setConstructExpressedComponent(constructExpressedComponents.get(id));
        document.setConstructKnockdownComponent(constructKnockdownComponents.get(id));
        document.setConstructRegulatoryRegion(constructRegulatoryRegions.get(id));
    }


}
