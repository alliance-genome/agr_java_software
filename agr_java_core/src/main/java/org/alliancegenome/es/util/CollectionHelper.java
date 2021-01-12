package org.alliancegenome.es.util;

import java.util.*;

public class CollectionHelper {

    public static Map<String, Set<String>> merge(Map<String,Set<String>> a, Map<String,Set<String>> b) {
        Map<String, Set<String>> map = new HashMap<>();
        Set<String> keys = new HashSet<>();

        keys.addAll(a.keySet());
        keys.addAll(b.keySet());

        for (String key: keys) {
            Set<String> values = new HashSet<>();
            if (a.get(key) != null) { values.addAll(a.get(key)); }
            if (b.get(key) != null) { values.addAll(b.get(key)); }
            map.put(key, values);
        }

        return map;
    }

}
