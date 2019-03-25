package org.alliancegenome.es.model.query;

import java.util.Arrays;
import java.util.StringJoiner;

public enum FieldFilter {
    GENE_NAME("geneName"),
    TERM_NAME("filter.term"),
    SPECIES("species"),
    FSPECIES("filter.species"),
    SPECIES_DEFAULT("species_default"),
    GENETIC_ENTITY("geneticEntity"),
    GENETIC_ENTITY_TYPE("geneticEntityType"),
    DISEASE("disease"),
    SOURCE("source"),
    STAGE("filter.stage"),
    ASSAY("filter.assay"),
    REFERENCE("reference"),
    FREFERENCE("filter.reference"),
    EVIDENCE_CODE("evidenceCode"),
    PHENOTYPE("termName"),
    ASSOCIATION_TYPE("associationType"),
    ORTHOLOG("filter.orthologGene"),
    EXPERIMENT("filter.experiment"),
    ORTHOLOG_SPECIES("filter.orthologGeneSpecies");
    public static final String FILTER_PREFIX = "filter.";
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

    public static boolean isFieldFilterValue(String value) {
        return Arrays.stream(values()).anyMatch(fieldFilter -> fieldFilter.getFullName().equalsIgnoreCase(value));
    }

    public static boolean hasFieldFilterPrefix(String value) {
        if (value == null)
            return false;
        return value.toLowerCase().startsWith(FILTER_PREFIX);
    }

    public String getName() {
        return name;
    }

    public String getFullName() {
        return FILTER_PREFIX + name;
    }
}
