package org.alliancegenome.cacher.config;

public enum Caches {

    GeneCache("gene"),
    GeneExpressionCache("geneExpression"),
    GenePhenotypeCache("genePhenotype"),
    AlleleCache("allele"),
    SpeciesCache("species"),
    GeneAlleleCache("geneAllele"),
    TaxonAlleleCache("taxonAllele"),
    ;

    private String cacheName;

    Caches(String cacheName) {
        this.cacheName = cacheName;
    }

    public String getCacheName() {
        return cacheName;
    }

}
