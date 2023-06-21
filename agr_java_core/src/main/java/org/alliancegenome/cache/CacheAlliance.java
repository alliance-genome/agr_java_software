package org.alliancegenome.cache;

import org.alliancegenome.api.entity.AlleleVariantSequence;
import org.alliancegenome.api.entity.CacheStatus;
import org.alliancegenome.core.ExpressionDetail;
import org.alliancegenome.neo4j.entity.DiseaseAnnotation;
import org.alliancegenome.neo4j.entity.PhenotypeAnnotation;
import org.alliancegenome.neo4j.entity.PrimaryAnnotatedEntity;
import org.alliancegenome.neo4j.entity.node.Allele;
import org.alliancegenome.neo4j.entity.node.ECOTerm;
import org.alliancegenome.neo4j.entity.node.InteractionGeneJoin;
import org.alliancegenome.neo4j.view.HomologView;

public enum CacheAlliance {

	// Rational for choosing memory sizes
	// Max 1G
	// Optimum (mean * 100) + (1 * max) (at least we can fit the largest into memory

	//GENE(),
	GENE_EXPRESSION(ExpressionDetail.class, (31586 * 100) + 1023787), // min: 791 max: 1023787 mean: 31586
	GENE_PHENOTYPE(PhenotypeAnnotation.class, (12584 * 100) + 956783), // min: 507 max: 956783 mean: 12584
	GENE_INTERACTION(InteractionGeneJoin.class, (134649 * 100) + 20188384), // min: 3354 max: 20188384 mean: 134649
	GENE_ORTHOLOGY(HomologView.class, (18912 * 100) + 2020141), // min: 905 max: 2020141 mean: 18912
	GENE_PARALOGY(HomologView.class, (18912 * 100) + 2020141), // min: 905 max: 2020141 mean: 18912
	GENE_PURE_AGM_PHENOTYPE(PrimaryAnnotatedEntity.class, 10_000_000), // Need to run the stats on this cache
	GENE_ASSOCIATION_MODEL_GENE(PrimaryAnnotatedEntity.class, (4073 * 100) + 1468636), // min: 492 max: 1468636 mean: 4073
	//GENE_ALLELE(),
	//GENE_DISEASE_ANNOTATION(),

	ALLELE_GENE(Allele.class, (3944 * 100) + 741634), // min: 607 max: 741634 mean: 3944
	ALLELE_VARIANT_SEQUENCE_GENE(AlleleVariantSequence.class, (3944 * 100) + 741634), // min: 607 max: 741634 mean: 3944
	ALLELE_PHENOTYPE(PhenotypeAnnotation.class, (7400 * 100) + 680459), // min: 789 max: 680459	 mean: 7400
	ALLELE_DISEASE(DiseaseAnnotation.class, (3700 * 100) + 82728), // min: 1814 max: 82728 mean: 3700
	ALLELE_SPECIES(Allele.class, 1_000_000_000), // min: 655252 max: 209555003 mean: 691048194
	//ALLELE_TAXON(),

	//DISEASE(),
	DISEASE_ANNOTATION_MODEL_LEVEL_DISEASE(DiseaseAnnotation.class, (11767 * 100) + 448244), // min: 2364 max: 448244 mean: 11767
	DISEASE_ANNOTATION_GENE_LEVEL_GENE_DISEASE(DiseaseAnnotation.class, (151780 * 100) + 356240668), // min: 3 max: 356240668 mean: 151780
	DISEASE_ANNOTATION_ALLELE_LEVEL_ALLELE(DiseaseAnnotation.class, (17611 * 100) + 2146761), // min: 3 max: 2146761 mean: 17611

	DISEASE_ANNOTATION_MODEL_LEVEL_GENE(PrimaryAnnotatedEntity.class, (2833 * 100) + 145069), // min: 891 max: 145069 mean: 2833

	CACHING_STATS(CacheStatus.class, (638702 * 100) + 2302255), // min: 124 max: 2302255 mean: 638702
	ECO_MAP(ECOTerm.class, (17 * 100000)), // min: 3 max: 250 mean: 17
	CLOSURE_MAP(String.class, (127 * 10000)), // min: 12 max: 388 mean: 127

	SPECIES_ORTHOLOGY(HomologView.class, 1_000_000_000),
	SPECIES_SPECIES_ORTHOLOGY(HomologView.class, 1_000_000_000),
	SPECIES_SPECIES_PARALOGY(HomologView.class, 1_000_000_000),

	SITEMAP_GENE(String.class, (263483 * 100) + 280790), // min: 181157 max: 280790 mean: 263483
	SITEMAP_ALLELE(String.class, (281158 * 100) + 418277), // min: 7364 max: 418277 mean: 281158
	SITEMAP_DISEASE(String.class, (72621 * 100) + 72621), // min: 72621 max: 72621 mean: 72621
	;

	private String cacheName;
	private Class<?> clazz;
	private Integer cacheSize;

	// Class object that will get serialized into the cache
	CacheAlliance(Class<?> clazz, Integer cacheSize) {
		this.clazz = clazz;
		cacheName = name().toLowerCase();
		this.cacheSize = cacheSize > 1_000_000_000 || cacheSize == 0 ? 1_000_000_000 : cacheSize;
	}

	public String getCacheName() {
		return cacheName;
	}

	public Class<?> getClazz() {
		return clazz;
	}

	public Integer getCacheSize() {
		return cacheSize;
	}

	public static CacheAlliance getTypeByName(String name) {
		for (CacheAlliance type : values())
			if (type.cacheName.equals(name))
				return type;
		return null;
	}

}
