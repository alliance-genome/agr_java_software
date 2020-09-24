package org.alliancegenome.es.index.site.cache;

import java.util.*;

import org.alliancegenome.es.index.site.document.SearchableItemDocument;
import org.alliancegenome.neo4j.entity.node.DOTerm;

import lombok.*;

@Getter
@Setter
public class DiseaseDocumentCache extends IndexerCache {

    private Map<String, DOTerm> diseaseMap = new HashMap<>();
    private Map<String, Set<String>> speciesMap = new HashMap<>();
    private Map<String, Set<String>> diseaseGroupMap = new HashMap<>();
    private Map<String, Set<String>> parentNameMap = new HashMap<>();

    public void addCachedFields(Iterable<SearchableItemDocument> diseaseDocuments) {
        for (SearchableItemDocument diseaseDocument : diseaseDocuments) {
           String id =  diseaseDocument.getPrimaryKey();

           super.addCachedFields(diseaseDocument);

           diseaseDocument.setAssociatedSpecies(speciesMap.get(id));
           diseaseDocument.setDiseaseGroup(diseaseGroupMap.get(id));
           diseaseDocument.setParentDiseaseNames(parentNameMap.get(id));
           diseaseDocument.setAssociatedSpecies(speciesMap.get(id));
            
        }
    }

}
