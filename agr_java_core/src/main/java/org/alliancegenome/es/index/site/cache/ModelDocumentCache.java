package org.alliancegenome.es.index.site.cache;

import java.util.HashMap;
import java.util.Map;

import org.alliancegenome.es.index.site.document.ModelDocument;
import org.alliancegenome.neo4j.entity.node.AffectedGenomicModel;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ModelDocumentCache extends IndexerCache {

    private Map<String, AffectedGenomicModel> modelMap = new HashMap<>();;

    public void addCachedFields(Iterable<ModelDocument> modelDocuments) {
        for (ModelDocument document: modelDocuments) {
            addCachedFields(document);
        }
    }

    public void addCachedFields(ModelDocument document) {
        super.addCachedFields(document);
    }

}
