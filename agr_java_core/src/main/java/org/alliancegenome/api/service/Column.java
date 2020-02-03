package org.alliancegenome.api.service;

public enum Column {

    MOLECULE_TYPE(Table.INTERACTION),
    INTERACTOR_GENE(Table.INTERACTION),
    INTERACTOR_SPECIES(Table.INTERACTION),
    INTERACTOR_MOLECULE_TYPE(Table.INTERACTION),
    INTERACTION_DETECTION_TYPE(Table.INTERACTION),
    INERACTION_SOURCE(Table.INTERACTION),
    REFERENCE(Table.INTERACTION),
    DISEASE_SPECIES(Table.DISEASE),
    DISEASE_ASSOCIATION(Table.DISEASE),
    EXPRESSION_SPECIES(Table.EXPRESSION),

    ASSOCIATES_GENES_SPECIES(Table.ASSOCIATED_GENE),
    ASSOCIATES_GENES_ASSOCIATION(Table.ASSOCIATED_GENE),
    ALLELE_SPECIES(Table.ALLELE),
    ALLELE_ASSOCIATION(Table.ALLELE),
    MODEL_SPECIES(Table.MODEL),

    ;

    private Table table;

    Column(Table table) {
        this.table = table;
    }

    public Table getTable() {
        return table;
    }
}
