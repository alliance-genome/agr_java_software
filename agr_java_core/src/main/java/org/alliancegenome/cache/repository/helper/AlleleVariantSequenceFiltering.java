package org.alliancegenome.cache.repository.helper;

import java.util.Set;
import java.util.stream.Collectors;

import org.alliancegenome.api.entity.AlleleVariantSequence;
import org.alliancegenome.es.model.query.FieldFilter;

public class AlleleVariantSequenceFiltering extends AnnotationFiltering<AlleleVariantSequence> {


	private static final FilterFunction<AlleleVariantSequence, String> alleleFilter =
			(alleleVariantSequence, value) ->{
		if(alleleVariantSequence.getAllele()!=null && alleleVariantSequence.getAllele().getSymbol()!=null)
				return FilterFunction.contains(alleleVariantSequence.getAllele().getSymbol(), value);
		return false;
			} ;

	private static final FilterFunction<AlleleVariantSequence, String> alleleCategoryFilter =
			(alleleVariantSequence, value) -> {
				if(alleleVariantSequence.getAllele()!=null && alleleVariantSequence.getAllele().getCategory()!=null){
				return FilterFunction.fullMatchMultiValueOR(alleleVariantSequence.getAllele().getCategory().trim(), value);}
				return false;
	};

	private static final FilterFunction<AlleleVariantSequence, String> synonymFilter =
			(alleleVariantSequence, value) -> {
				if(alleleVariantSequence.getAllele()!=null && alleleVariantSequence.getAllele().getSynonymsList()!=null &&
						alleleVariantSequence.getAllele().getSynonymsList().size()>0) {
					Set<Boolean> filteringPassed = alleleVariantSequence.getAllele().getSynonymsList().stream()
							.map(synonym -> FilterFunction.contains(synonym.toLowerCase().trim(), value))
							.collect(Collectors.toSet());
					return !filteringPassed.isEmpty() && filteringPassed.contains(true);
				}

				return false;
			};

	private static final FilterFunction<AlleleVariantSequence, String> variantTypeFilter =
			(alleleVariantSequence, value) -> {
				if (alleleVariantSequence.getVariant() != null &&
						alleleVariantSequence.getVariant().getVariantType()!=null &&
						alleleVariantSequence.getVariant().getVariantType().getName()!=null) {

					return FilterFunction.fullMatchMultiValueOR(alleleVariantSequence.getVariant().getVariantType().getName().trim(), value.trim());
				}
				return false;
			};

	private static final FilterFunction<AlleleVariantSequence, String> hgvsgNameFilter =
			(alleleVariantSequence, value) -> {
				if(alleleVariantSequence.getVariant()!=null && alleleVariantSequence.getVariant().getHgvsNomenclature()!=null
						&& !alleleVariantSequence.getVariant().getHgvsNomenclature().equals("")){
					return	 alleleVariantSequence.getVariant().getHgvsNomenclature().toLowerCase().trim().contains(value.toLowerCase());}
				return false;
			};
	private static final FilterFunction<AlleleVariantSequence, String> alleleHasPhenotypeFilter =
			(alleleVariantSequence, value) -> {
				if(alleleVariantSequence.getAllele()!=null && alleleVariantSequence.getAllele().hasPhenotype()!=null)
					return	 FilterFunction.fullMatchMultiValueOR(alleleVariantSequence.getAllele().hasPhenotype().toString(), value);
				return false;
			};
	private static final FilterFunction<AlleleVariantSequence, String> alleleHasDiseaseFilter =
			(alleleVariantSequence, value) -> {
				if(alleleVariantSequence.getAllele()!=null && alleleVariantSequence.getAllele().hasDisease()!=null)
					return	 FilterFunction.fullMatchMultiValueOR(alleleVariantSequence.getAllele().hasDisease().toString(), value);
				return false;
			};
	private static final FilterFunction<AlleleVariantSequence, String> molecularConsequenceFilter =
			(alleleVariantSequence, value) -> {
				if (alleleVariantSequence.getConsequence() != null &&
						alleleVariantSequence.getConsequence().getMolecularConsequences() != null &&
						alleleVariantSequence.getConsequence().getMolecularConsequences().size()>0)
					return FilterFunction.fullMatchMultiValueOR(Set.copyOf(alleleVariantSequence.getConsequence().getMolecularConsequences()), value);
				return false;
			};

	private static final FilterFunction<AlleleVariantSequence, String> locationFilter =
			(alleleVariantSequence, value) -> {
				if (alleleVariantSequence.getConsequence() != null &&
						alleleVariantSequence.getConsequence().getLocation()!=null &&
						!alleleVariantSequence.getConsequence().getLocation().equals("")) {
					return alleleVariantSequence.getConsequence().getLocation().toLowerCase().contains(value.toLowerCase().trim());
				} return false;
			};

	private static final FilterFunction<AlleleVariantSequence, String> variantImpactFilter =
			(alleleVariantSequence, value) -> {
				if (alleleVariantSequence.getConsequence() != null && alleleVariantSequence.getConsequence().getImpact()!=null
						&& !alleleVariantSequence.getConsequence().getImpact().equals(""))
					return FilterFunction.fullMatchMultiValueOR(alleleVariantSequence.getConsequence().getImpact(), value);
				return false;
			};

	private static final FilterFunction<AlleleVariantSequence, String> sequenceFeatureTypeFilter =
			(alleleVariantSequence, value) -> {
				if (alleleVariantSequence.getConsequence() != null &&
						alleleVariantSequence.getConsequence().getSequenceFeatureType()!=null &&
						!alleleVariantSequence.getConsequence().getSequenceFeatureType().equals(""))
					return FilterFunction.fullMatchMultiValueOR(alleleVariantSequence.getConsequence().getSequenceFeatureType(), value);
				return false;
			};

	private static final FilterFunction<AlleleVariantSequence, String> sequenceFeatureFilter =
			(alleleVariantSequence, value) -> {
				if (alleleVariantSequence.getConsequence() != null && alleleVariantSequence.getConsequence().getTranscript()!=null
						&& alleleVariantSequence.getConsequence().getTranscript().getName()!=null &&
						!alleleVariantSequence.getConsequence().getTranscript().getName().equals(""))
					return FilterFunction.contains(alleleVariantSequence.getConsequence().getTranscript().getName(), value);
				return false;
			};

	private static final FilterFunction<AlleleVariantSequence, String> variantPolyphenFilter =
			(alleleVariantSequence, value) -> {
				if (alleleVariantSequence.getConsequence() != null &&
						alleleVariantSequence.getConsequence().getPolyphenPrediction()!=null && !alleleVariantSequence.getConsequence().getPolyphenPrediction().equals(""))
					return FilterFunction.fullMatchMultiValueOR(alleleVariantSequence.getConsequence().getPolyphenPrediction(), value);
				return false;
			};

	private static final FilterFunction<AlleleVariantSequence, String> variantSiftFilter =
			(alleleVariantSequence, value) -> {
				if (alleleVariantSequence.getConsequence() != null && alleleVariantSequence.getConsequence().getSiftPrediction()!=null
						&& !alleleVariantSequence.getConsequence().getSiftPrediction().equals(""))
					return FilterFunction.fullMatchMultiValueOR(alleleVariantSequence.getConsequence().getSiftPrediction(), value);
				return false;
			};


	public AlleleVariantSequenceFiltering() {
		filterFieldMap.put(FieldFilter.SYMBOL, alleleFilter);
		filterFieldMap.put(FieldFilter.SYNONYMS, synonymFilter);
		filterFieldMap.put(FieldFilter.ALLELE_CATEGORY, alleleCategoryFilter);
		filterFieldMap.put(FieldFilter.HAS_PHENOTYPE, alleleHasPhenotypeFilter);
		filterFieldMap.put(FieldFilter.HAS_DISEASE, alleleHasDiseaseFilter);
		filterFieldMap.put(FieldFilter.VARIANT_TYPE, variantTypeFilter);
		filterFieldMap.put(FieldFilter.VARIANT_HGVS_G, hgvsgNameFilter);
		filterFieldMap.put(FieldFilter.MOLECULAR_CONSEQUENCE, molecularConsequenceFilter);
		filterFieldMap.put(FieldFilter.VARIANT_IMPACT, variantImpactFilter);
		filterFieldMap.put(FieldFilter.VARIANT_POLYPHEN, variantPolyphenFilter);
		filterFieldMap.put(FieldFilter.VARIANT_SIFT, variantSiftFilter);
		filterFieldMap.put(FieldFilter.SEQUENCE_FEATURE_TYPE, sequenceFeatureTypeFilter);
		filterFieldMap.put(FieldFilter.SEQUENCE_FEATURE, sequenceFeatureFilter);
		filterFieldMap.put(FieldFilter.VARIANT_LOCATION, locationFilter);
	}

}

