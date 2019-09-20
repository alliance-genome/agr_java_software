package org.alliancegenome.es.index.site.cache;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import lombok.Getter;
import lombok.Setter;
import org.alliancegenome.es.index.site.document.SearchableItemDocument;

@Getter
@Setter
public class IndexerCache {

    protected Map<String, Set<String>> diseases = new HashMap<>();
    protected Map<String, Set<String>> diseasesAgrSlim = new HashMap<>();
    protected Map<String, Set<String>> diseasesWithParents = new HashMap<>();
    protected Map<String, Set<String>> alleles = new HashMap<>();
    protected Map<String, Set<String>> genes = new HashMap<>();
    protected Map<String, Set<String>> phenotypeStatements = new HashMap<>();

    protected void addCachedFields(SearchableItemDocument document) {
        String id = document.getPrimaryId();

        document.setAlleles(alleles.get(id));
        document.setDiseases(diseases.get(id));
        document.setDiseasesAgrSlim(diseasesAgrSlim.get(id));
        document.setDiseasesWithParents(diseasesWithParents.get(id));
        document.setGenes(genes.get(id));
        document.setPhenotypeStatements(phenotypeStatements.get(id));

    }

}
