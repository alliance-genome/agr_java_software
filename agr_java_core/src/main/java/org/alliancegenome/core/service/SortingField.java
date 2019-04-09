package org.alliancegenome.core.service;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum SortingField {

    SPECIES_PHYLOGENETIC, EXPERIMENT_ORTHOLOGY, GENESYMBOL, ALLELESYMBOL, SPECIES, DISEASE, ASSOCIATIONTYPE, PHENOTYPE;

    public static SortingField getSortingField(String name) {
        return Arrays.stream(values())
                .filter(sortingField -> sortingField.name().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    public static boolean isValidSortingFieldValue(String value) {
        if (value == null || StringUtils.isBlank(value))
            return true;
        return getSortingField(value) != null;
    }

    public static List<SortingField> getAllValues() {
        return Arrays.stream(values()).collect(Collectors.toList());
    }
}
