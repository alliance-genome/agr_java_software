package org.alliancegenome.es.index.site.cache;

import java.util.HashMap;
import java.util.Map;

import org.alliancegenome.es.index.site.document.AlleleDocument;
import org.alliancegenome.neo4j.entity.node.Allele;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AlleleDocumentCache extends IndexerCache {

    private Map<String, Allele> alleleMap = new HashMap<>();

    public void addCachedFields(Iterable<AlleleDocument> alleleDocuments) {

        for (AlleleDocument alleleDocument : alleleDocuments) {
            String id = alleleDocument.getPrimaryKey();

            alleleDocument.setDiseases(diseases.get(id));
            alleleDocument.setDiseasesAgrSlim(diseasesAgrSlim.get(id));
            alleleDocument.setDiseasesWithParents(diseasesWithParents.get(id));
            alleleDocument.setGenes(genes.get(id));
            alleleDocument.setPhenotypeStatements(phenotypeStatements.get(id));
        }

    }

}
