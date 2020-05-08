package org.alliancegenome.cache;

import org.alliancegenome.api.entity.CacheStatus;
import org.alliancegenome.core.ExpressionDetail;
import org.alliancegenome.neo4j.entity.DiseaseAnnotation;
import org.alliancegenome.neo4j.entity.PhenotypeAnnotation;
import org.alliancegenome.neo4j.entity.PrimaryAnnotatedEntity;
import org.alliancegenome.neo4j.entity.node.Allele;
import org.alliancegenome.neo4j.entity.node.ECOTerm;
import org.alliancegenome.neo4j.entity.node.InteractionGeneJoin;
import org.alliancegenome.neo4j.view.OrthologView;

public enum CacheAlliance {

    //GENE(),
    GENE_EXPRESSION(ExpressionDetail.class),
    GENE_PHENOTYPE(PhenotypeAnnotation.class),
    GENE_INTERACTION(InteractionGeneJoin.class),
    GENE_ORTHOLOGY(OrthologView.class),
    GENE_PURE_AGM_PHENOTYPE(PrimaryAnnotatedEntity.class),
    GENE_ASSOCIATION_MODEL_GENE(PrimaryAnnotatedEntity.class),
    //GENE_ALLELE(),
    //GENE_DISEASE_ANNOTATION(),
    
    ALLELE_GENE(Allele.class),
    ALLELE_PHENOTYPE(PhenotypeAnnotation.class),
    ALLELE_DISEASE(DiseaseAnnotation.class),
    ALLELE_SPECIES(Allele.class),
    //ALLELE_TAXON(),
    
    //DISEASE(),
    DISEASE_ANNOTATION_MODEL_LEVEL_MODEL(DiseaseAnnotation.class),
    DISEASE_ANNOTATION_GENE_LEVEL_GENE_DISEASE(DiseaseAnnotation.class),
    DISEASE_ANNOTATION_ALLELE_LEVEL_ALLELE(DiseaseAnnotation.class),
    
    DISEASE_ANNOTATION_MODEL_LEVEL_GENE(PrimaryAnnotatedEntity.class),

    CACHING_STATS(CacheStatus.class),
    ECO_MAP(ECOTerm.class),
    CLOSURE_MAP(String.class),

    SPECIES_ORTHOLOGY(OrthologView.class),
    SPECIES_SPECIES_ORTHOLOGY(OrthologView.class),

    SITEMAP_GENE(String.class),
    SITEMAP_ALLELE(String.class),
    SITEMAP_DISEASE(String.class);
    
    private String cacheName;
    private Class<?> clazz;

    // Class object that will get serialized into the cache
    CacheAlliance(Class<?> clazz) {
        this.clazz = clazz;
        cacheName = name().toLowerCase();
    }

    public String getCacheName() {
        return cacheName;
    }
    
    public Class<?> getClazz() {
        return clazz;
    }

    public static CacheAlliance getTypeByName(String name) {
        for (CacheAlliance type : values())
            if (type.cacheName.equals(name))
                return type;
        return null;
    }

}
