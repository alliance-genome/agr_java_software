package org.alliancegenome.api.service;


public enum Table {

    INTERACTION(Page.GENE),
    PHENOTYPE(Page.GENE),
    DISEASE(Page.GENE),
    EXPRESSION(Page.GENE),
    ALLELE_GENE(Page.GENE),
    ALLELE_VARIANT_GENE(Page.GENE),
    TRANSGENIC_ALLELE(Page.GENE),

    ASSOCIATED_GENE(Page.DISEASE),
    ALLELE(Page.DISEASE),
    MODEL(Page.DISEASE),

    ALLELE_DISEASE(Page.ALLELE),
    ;

    private Page page;

    Table(Page page) {
        this.page = page;
    }

    public Page getPage() {
        return page;
    }
}
