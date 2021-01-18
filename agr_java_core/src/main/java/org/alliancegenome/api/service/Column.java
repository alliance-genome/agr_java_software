package org.alliancegenome.api.service;

public enum Column {

    MOLECULE_TYPE(Table.INTERACTION, true),
    INTERACTOR_GENE(Table.INTERACTION, true),
    INTERACTOR_SPECIES(Table.INTERACTION, true),
    INTERACTOR_MOLECULE_TYPE(Table.INTERACTION, true),
    INTERACTOR_GENE_SYMBOL(Table.INTERACTION, true),
    INTERACTOR_SOURCE(Table.INTERACTION, true),
    INTERACTOR_REFERENCE(Table.INTERACTION, true),
    INTERACTOR_DETECTION_METHOD(Table.INTERACTION, true),
    INTERACTION_DETECTION_TYPE(Table.INTERACTION, true),
    INERACTION_SOURCE(Table.INTERACTION, true),

    TG_ALLELE_SPECIES(Table.TRANSGENIC_ALLELE, true),

    DISEASE_SPECIES(Table.DISEASE, true),
    DISEASE_ASSOCIATION(Table.DISEASE, true),
    EXPRESSION_SPECIES(Table.EXPRESSION, true),

    GENE_ALLELE_VARIANT_TYPE(Table.ALLELE_GENE, true),
    GENE_ALLELE_VARIANT_CONSEQUENCE(Table.ALLELE_GENE, true),
    GENE_ALLELE_CATEGORY(Table.ALLELE_GENE, true),

    GENE_ALLELE_VARIANT_SEQUENCE_TYPE(Table.ALLELE_VARIANT_GENE, true),
    GENE_ALLELE_VARIANT_SEQUENCE_CONSEQUENCE(Table.ALLELE_VARIANT_GENE, true),
    GENE_ALLELE_VARIANT_SEQUENCE_CATEGORY(Table.ALLELE_VARIANT_GENE, true),

    ASSOCIATES_GENES_SPECIES(Table.ASSOCIATED_GENE, true),
    ASSOCIATES_GENES_ASSOCIATION(Table.ASSOCIATED_GENE, true),
    ALLELE_SPECIES(Table.ALLELE, true),
    ALLELE_ASSOCIATION(Table.ALLELE, true),
    ASSOCIATION_TYPE(Table.ALLELE_DISEASE, true),
    MODEL_SPECIES(Table.MODEL, true),
    MODEL_ASSOCIATION_TYPE(Table.MODEL, true),
    ;


    private Table table;
    private boolean filterElement;

    Column(Table table) {
        this.table = table;
    }

    Column(Table table, boolean filterElement) {
        this.table = table;
        this.filterElement = filterElement;
    }

    public Table getTable() {
        return table;
    }

    public boolean isFilterElement() {
        return filterElement;
    }
}

