package org.alliancegenome.es.index.site.cache;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IndexerCache {

    protected Map<String, Set<String>> diseases = new HashMap<>();
    protected Map<String, Set<String>> diseasesAgrSlim = new HashMap<>();
    protected Map<String, Set<String>> diseasesWithParents = new HashMap<>();
    protected Map<String, Set<String>> alleles = new HashMap<>();
    protected Map<String, Set<String>> genes = new HashMap<>();
    protected Map<String, Set<String>> phenotypeStatements = new HashMap<>();

}
