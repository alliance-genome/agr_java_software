package org.alliancegenome.cache;

import org.eclipse.jgit.util.StringUtils;

public enum CacheAlliance {

    // @formatter:off
    GENE(),
        EXPRESSION(GENE),
        PHENOTYPE(GENE),
        INTERACTION(GENE),
        ORTHOLOGY(GENE),
        ALLELE(GENE),
            SPECIES(ALLELE),
            TAXON(ALLELE),
        GENE_DISEASE_ANNOTATION(GENE),
    DISEASE(),
         DISEASE_ANNOTATION(DISEASE),
    ;
    // @formatter:on

    private String cacheName;

    CacheAlliance() {
        cacheName = name().toLowerCase();
    }

    public String getCacheName() {
        return cacheName;
    }

    private CacheAlliance parent = null;

    CacheAlliance(CacheAlliance parents) {
        this.parent = parents;
        cacheName = parent.getCacheName();
        createCacheName();
    }

    private void createCacheName() {
        cacheName += StringUtils.capitalize(name().toLowerCase());
    }

    public boolean is(CacheAlliance type) {
        if (type == null)
            return false;
        if (this.equals(type))
            return true;
        if (parent.equals(type))
            return true;
        if (parent.hasParent()) {
            if (parent.is(type))
                return true;
        }
        return false;
    }

    private boolean hasParent() {
        return parent != null;
    }

}
