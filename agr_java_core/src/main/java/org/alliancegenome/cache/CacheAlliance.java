package org.alliancegenome.cache;

public enum CacheAlliance {

    GENE(),
    GENE_EXPRESSION(),
    GENE_PHENOTYPE(),
    GENE_INTERACTION(),
    GENE_ORTHOLOGY(),
    GENE_MODEL(),
    GENE_ALLELE(),
    GENE_DISEASE_ANNOTATION(),
    GENE_SITEMAP(),

    ALLELE(),
    ALLELE_PHENOTYPE(),
    ALLELE_DISEASE(),
    ALLELE_SPECIES(),
    ALLELE_TAXON(),
    ALLELE_SITEMAP(),

    DISEASE(),
    DISEASE_MODEL_ANNOTATION(),
    DISEASE_ANNOTATION(),
    DISEASE_ALLELE_ANNOTATION(),
    DISEASE_SITEMAP(),

    CACHING_STATS(),
    ECO_MAP(),
    CLOSURE_MAP(),
    GENE_PURE_AGM_PHENOTYPE(),
    DISEASE_MODEL_GENE_ANNOTATION(),

    SPECIES_ORTHOLOGY,
    SPECIES_SPECIES_ORTHOLOGY;

    private String cacheName;

    CacheAlliance() {
        cacheName = name().toLowerCase();
    }

    public String getCacheName() {
        return cacheName;
    }

    public static CacheAlliance getTypeByName(String name) {
        for (CacheAlliance type : values())
            if (type.cacheName.equals(name))
                return type;
        return null;
    }

}
