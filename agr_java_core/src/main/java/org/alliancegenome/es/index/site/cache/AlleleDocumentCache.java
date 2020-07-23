package org.alliancegenome.es.index.site.cache;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.alliancegenome.es.index.site.document.SearchableItemDocument;
import org.alliancegenome.neo4j.entity.node.Allele;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AlleleDocumentCache extends IndexerCache {

    @Override
    public void addCachedFields(Iterable<SearchableItemDocument> alleleDocuments) {

        for (SearchableItemDocument document : alleleDocuments) {
            String id = document.getPrimaryKey();

            super.addCachedFields(document);
        }
    }

}
