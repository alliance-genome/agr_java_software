package org.alliancegenome.cache;

public enum CacheAlliance {

    GENE(),
    GENE_EXPRESSION(),
    GENE_PHENOTYPE(),
    GENE_INTERACTION(),
    GENE_ORTHOLOGY(),
    GENE_ASSOCIATION_MODEL_GENE(),
    GENE_ALLELE(),
    GENE_DISEASE_ANNOTATION(),
    GENE_SITEMAP(),

    ALLELE_GENE(),
    ALLELE_PHENOTYPE(),
    ALLELE_DISEASE(),
    ALLELE_SPECIES(),
    ALLELE_TAXON(),
    ALLELE_SITEMAP(),

    DISEASE(),
    DISEASE_ANNOTATION_MODEL_LEVEL_MODEL(),
    DISEASE_ANNOTATION_GENE_LEVEL_GENE_DISEASE(),
    DISEASE_ANNOTATION_ALLELE_LEVEL_ALLELE(),
    DISEASE_SITEMAP(),
    DISEASE_ANNOTATION_MODEL_LEVEL_GENE(),

    CACHING_STATS(),
    ECO_MAP(),
    CLOSURE_MAP(),
    GENE_PURE_AGM_PHENOTYPE(),

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
