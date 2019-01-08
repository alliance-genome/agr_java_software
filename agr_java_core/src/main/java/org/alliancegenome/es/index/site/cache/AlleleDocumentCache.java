package org.alliancegenome.es.index.site.cache;

import lombok.Getter;
import lombok.Setter;
import org.alliancegenome.es.index.site.document.AlleleDocument;
import org.alliancegenome.neo4j.entity.node.Allele;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class AlleleDocumentCache extends IndexerCache {

    private Map<String, Allele> alleleMap = new HashMap<>();

    public void addCachedFields(Iterable<AlleleDocument> alleleDocuments) {

        for (AlleleDocument alleleDocument : alleleDocuments) {
            String id = alleleDocument.getPrimaryKey();

            alleleDocument.setDiseases(diseases.get(id));
            alleleDocument.setGenes(genes.get(id));
            alleleDocument.setPhenotypeStatements(phenotypeStatements.get(id));

        }

    }

}
