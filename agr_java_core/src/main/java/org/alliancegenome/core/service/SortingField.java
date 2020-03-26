package org.alliancegenome.core.service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

public enum SortingField {

    SPECIES_PHYLOGENETIC, EXPERIMENT_ORTHOLOGY, GENESYMBOL, SPECIES, DISEASE, ASSOCIATIONTYPE, PHENOTYPE,
    SYMBOL, SYNONYM, ALLELESYMBOL, GENETIC_ENTITY, GENETIC_ENTITY_TYPE, INTERACTOR_GENE_SYMBOL,
    MOLECULE_TYPE, INTERACTOR_MOLECULE_TYPE, INTERACTOR_DETECTION_METHOD, INTERACTOR_SPECIES,
    EXPRESSION, STAGE, ASSAY, REFERENCE, GENE, LOCATION, DEFAULT,DISEASE_ALLELE_DEFAULT, ALLELE, MODEL, VARIANT, VARIANT_TYPE, VARIANT_CONSEQUENCE;

    public static SortingField getSortingField(String name) {
        return Arrays.stream(values())
                .filter(sortingField -> {
                    if (sortingField.name().equalsIgnoreCase(name))
                        return true;
                    // allow snake case to be recognized as well
                    // e.g. genetic_entity ~= geneticEntity
                    return sortingField.name().replace("_", "").equalsIgnoreCase(name);
                })
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
