package org.alliancegenome.es.index.site.cache;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.alliancegenome.es.index.site.document.DiseaseDocument;
import org.alliancegenome.neo4j.entity.node.DOTerm;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DiseaseDocumentCache extends IndexerCache {

    private Map<String, DOTerm> diseaseMap = new HashMap<>();
    private Map<String, Set<String>> speciesMap = new HashMap<>();
    private Map<String, Set<String>> diseaseGroupMap = new HashMap<>();
    private Map<String, Set<String>> parentNameMap = new HashMap<>();

    public void addCachedFields(Iterable<DiseaseDocument> diseaseDocuments) {
        for (DiseaseDocument diseaseDocument : diseaseDocuments) {
           String id =  diseaseDocument.getPrimaryKey();

           super.addCachedFields(diseaseDocument);

           diseaseDocument.setAssociatedSpecies(speciesMap.get(id));
           diseaseDocument.setDiseaseGroup(diseaseGroupMap.get(id));
           diseaseDocument.setParentDiseaseNames(parentNameMap.get(id));
           diseaseDocument.setAssociatedSpecies(speciesMap.get(id));
            
        }
    }

}
