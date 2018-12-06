package org.alliancegenome.es.index.site.cache;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Getter
@Setter
public class IndexerCache {

    protected Map<String, Set<String>> diseases = new HashMap<>();
    protected Map<String, Set<String>> features = new HashMap<>();
    protected Map<String, Set<String>> genes = new HashMap<>();
    protected Map<String, Set<String>> phenotypeStatements = new HashMap<>();

}
