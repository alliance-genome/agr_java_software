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

    private Map<String, Allele> alleleMap = new HashMap<>();
    private Map<String, Set<String>> variantTypesMap = new HashMap<>();
    private Map<String, Set<String>> molecularConsequenceMap = new HashMap<>();

    @Override
    public void addCachedFields(Iterable<SearchableItemDocument> alleleDocuments) {

        for (SearchableItemDocument document : alleleDocuments) {
            String id = document.getPrimaryKey();

            super.addCachedFields(document);

            if (molecularConsequenceMap.get(id) != null) {
                document.setMolecularConsequence(new HashSet<>());
                for (String consequence : molecularConsequenceMap.get(id)) {
                    document.getMolecularConsequence().addAll(Arrays.asList(consequence.split(",")));
                }
            }

            if (variantTypesMap.get(id) == null) {
                Set<String> defaultValue = new HashSet<>();
                defaultValue.add("unreported");
                document.setVariantTypes(defaultValue);
            } else {
                document.setVariantTypes(variantTypesMap.get(id));
            }
        }

    }

}
