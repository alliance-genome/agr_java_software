package org.alliancegenome.indexer.entity;

public enum SpeciesType {
    HUMAN("Homo sapiens", "HUMAN","NCBITaxon:9606"),
    RAT("Rattus norvegicus","RGD", "NCBITaxon:10116"),
    MOUSE("Mus musculus", "MGI","NCBITaxon:10090"),
    ZEBRAFISH("Danio rerio", "ZFIN","NCBITaxon:7955"),
    FLY("Drosophila melanogaster","FB", "NCBITaxon:7227"),
    WORM("Caenorhabditis elegans", "WB","NCBITaxon:6239"),
    YEAST("Saccharomyces cerevisiae", "SGD","NCBITaxon:4932");

    String name;
    String taxonID;
    String displayName;

    public String getName() {
        return name;
    }

    public String getTaxonID() {
        return taxonID;
    }

    public String getDisplayName() {
        return displayName;
    }

    SpeciesType(String name, String displayName, String taxinID) {
        this.name = name;
        this.displayName = displayName;
        this.taxonID = taxinID;
    }

    public static SpeciesType getTypeByName(String name) {
        for (SpeciesType type : values())
            if (type.name.equals(name))
                return type;
        return null;
    }
}
