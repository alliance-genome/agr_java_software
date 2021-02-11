package org.alliancegenome.es.model.query;

import java.util.*;

public enum FieldFilter {
    GENE_NAME("geneName"),
    TERM_NAME("filter.term"),
    SPECIES("species"),
    FSPECIES("filter.species"),
    SPECIES_DEFAULT("species_default"),
    GENETIC_ENTITY("geneticEntity"),
    GENETIC_ENTITY_TYPE("geneticEntityType"),
    SOURCE("source"),
    STAGE("filter.stage"),
    ASSAY("filter.assay"),
    FREFERENCE("filter.reference"),
    EVIDENCE_CODE("evidenceCode"),
    PHENOTYPE("termName"),
    ASSOCIATION_TYPE("associationType"),
    ORTHOLOG("filter.orthologGene"),
    EXPERIMENT("filter.experiment"),
    ORTHOLOG_SPECIES("filter.orthologGeneSpecies"),
    SYMBOL("filter.symbol"),
    DISEASE("filter.disease"),
    SYNONYMS("filter.synonyms"),
    //those for molecular interaction
    INTERACTOR_MOLECULE_TYPE("filter.interactorMoleculeType"),
    INTERACTOR_GENE_SYMBOL("filter.interactorGeneSymbol"),
    DETECTION_METHOD("filter.detectionMethod"),
    INTERACTOR_SPECIES("filter.interactorSpecies"),
    VARIANT_TYPE("filter.variantType"),
    CONSEQUENCE_TYPE("filter.consequenceType"),
    VARIANT_POLYPHEN("filter.variantPolyphen"),
    VARIANT_SIFT("filter.variantSift"),
    MOLECULE_TYPE("filter.moleculeType"),
    JOIN_TYPE("filter.joinType"),
    //those for genetic interaction
    
    BASED_ON_GENE("filter.basedOnGeneSymbol"),
    INCLUDE_NEGATION("includeNegation"),
    ALLELE("allele"),
    MODEL_NAME("modelName"),
    MOLECULAR_CONSEQUENCE("filter.molecularConsequence"),
    SEQUENCE_FEATURE_TYPE("filter.sequenceFeatureType"),
    ASSOCIATED_GENE("filter.associatedGeneSymbol"),
    VARIANT_HGVS_G("filter.hgvsgName"),
    ALLELE_CATEGORY("filter.alleleCategory"),
    VARIANT_IMPACT("filter.variantImpact"),
    CONSTRUCT_SYMBOL("filter.allele-construct-symbol"),
    CONSTRUCT_REGULATED_GENE("filter.allele-construct-regulated-gene"),
    CONSTRUCT_EXPRESSED_GENE("filter.allele-construct-expressed-gene"),
    CONSTRUCT_TARGETED_GENE("filter.allele-construct-targeted-gene"),
    HAS_PHENOTYPE("filter.hasPhenotype"),
    HAS_DISEASE("filter.hasDisease"),
    TRANSGENE_HAS_PHENOTYPE("filter.transgene-has-phenotype"),
    TRANSGENE_HAS_DISEASE("filter.transgene-has-disease"),
    STRINGENCY("filter.stringency"),
    ORTHOLOGY_METHOD("method"),
    ORTHOLOGY_TAXON("taxon"),
    INDEX_NAME("indexName");
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
