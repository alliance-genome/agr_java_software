package org.alliancegenome.es.model.query;

import java.util.StringJoiner;

public enum FieldFilter {
    GENE_NAME("geneName"),
    SPECIES("species"),
    GENETIC_ENTITY("geneticEntity"),
    GENETIC_ENTITY_TYPE("geneticEntityType"),
    DISEASE("disease"),
    SOURCE("source"),
    REFERENCE("reference"),
    EVIDENCE_CODE("evidenceCode"),
    ASSOCIATION_TYPE("associationType"),;
    private String name;

    FieldFilter(String name) {
        this.name = name;
    }

    public static FieldFilter getFieldFilterByName(String name) {
        if (name == null)
            return null;
        for (FieldFilter sort : values()) {
            if (sort.name.equals(name))
                return sort;
        }
        return null;
    }

    public static String getAllValues() {
        StringJoiner values = new StringJoiner(",");
        for (FieldFilter sorting : values())
            values.add(sorting.name);
        return values.toString();
    }

    public String getName() {
        return name;
    }
}
