package org.alliancegenome.es.index.site.cache;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.alliancegenome.es.index.site.document.AlleleDocument;
import org.alliancegenome.neo4j.entity.node.Allele;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AlleleDocumentCache extends IndexerCache {

    private Map<String, Allele> alleleMap = new HashMap<>();
    private Map<String, Set<String>> variantTypesMap = new HashMap<>();
    private Map<String, Set<String>> molecularConsequenceMap = new HashMap<>();

    public void addCachedFields(Iterable<AlleleDocument> alleleDocuments) {

        for (AlleleDocument alleleDocument : alleleDocuments) {
            String id = alleleDocument.getPrimaryKey();

            super.addCachedFields(alleleDocument);

            if (molecularConsequenceMap.get(id) != null) {
                alleleDocument.setMolecularConsequence(new HashSet<>());
                for (String consequence : molecularConsequenceMap.get(id)) {
                    alleleDocument.getMolecularConsequence().addAll(Arrays.asList(consequence.split(",")));
                }
            }

            if (variantTypesMap.get(id) == null) {
                Set<String> defaultValue = new HashSet<>();
                defaultValue.add("unreported");
                alleleDocument.setVariantTypes(defaultValue);
            } else {
                alleleDocument.setVariantTypes(variantTypesMap.get(id));
            }
        }

    }

}
