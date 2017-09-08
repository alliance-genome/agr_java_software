package org.alliancegenome.indexer.entity;

public enum SpeciesType {
    HUMAN("Homo sapiens", "NCBITaxon:9606"),
    RAT("Rattus norvegicus", "NCBITaxon:10116"),
    MOUSE("Mus musculus", "NCBITaxon:10090"),
    ZEBRAFISH("Danio rerio", "NCBITaxon:7955"),
    FLY("Drosophila melanogaster", "NCBITaxon:7227"),
    WORM("Caenorhabditis elegans", "NCBITaxon:6239"),
    YEAST("Saccharomyces cerevisiae", "NCBITaxon:4932");

    String name;
    String taxinID;

    SpeciesType(String name, String taxinID) {
        this.name = name;
        this.taxinID = taxinID;
    }

    public static SpeciesType getTypeByName(String name) {
        for (SpeciesType type : values())
            if (type.name.equals(name))
                return type;
        return null;
    }
}
