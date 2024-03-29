package org.alliancegenome.cache.repository.helper;

import static org.alliancegenome.neo4j.entity.SpeciesType.NCBITAXON;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.alliancegenome.es.model.query.FieldFilter;
import org.alliancegenome.neo4j.entity.node.Allele;
import org.alliancegenome.neo4j.entity.node.Construct;
import org.alliancegenome.neo4j.entity.node.GeneticEntity;
import org.alliancegenome.neo4j.entity.node.NonBGIConstructComponent;

public class AlleleFiltering extends AnnotationFiltering<Allele> {


	private static FilterFunction<Allele, String> alleleFilter =
			(allele, value) -> FilterFunction.contains(allele.getSymbolText(), value);

	private static FilterFunction<Allele, String> transgenicAllelePhenotypeFilter =
			(allele, value) -> FilterFunction.contains(allele.hasPhenotype().toString(), value);

	private static FilterFunction<Allele, String> transgenicAlleleDiseaseFilter =
			(allele, value) -> FilterFunction.contains(allele.hasDisease().toString(), value);

	private static FilterFunction<Allele, String> alleleCategoryFilter =
			(allele, value) -> FilterFunction.fullMatchMultiValueOR(allele.getCategory(), value);

	private static FilterFunction<Allele, String> speciesFilter =
			(allele, value) -> {
				if (allele.getSpecies() != null) {
					if (value.startsWith(NCBITAXON))
						return FilterFunction.fullMatchMultiValueOR(allele.getSpecies().getType().getTaxonID(), value);
					else
						return FilterFunction.fullMatchMultiValueOR(allele.getSpecies().getName(), value);
				}
				return false;
			};

	private static FilterFunction<Allele, String> synonymFilter =
			(allele, value) -> {
					Set<Boolean> filteringPassed=null;
					if( allele.getSynonymsList()!=null ) {
						filteringPassed = allele.getSynonymsList().stream()
								.filter(Objects::nonNull)
								.map(synonym -> FilterFunction.contains(synonym.toLowerCase().trim(), value))
								.collect(Collectors.toSet());

					}

				return filteringPassed!=null && !filteringPassed.isEmpty() && filteringPassed.contains(true);

			};

	private static FilterFunction<Allele, String> diseaseFilter =
			(allele, value) -> {
				Set<Boolean> filteringPassed = allele.getDiseases().stream()
						.map(term -> FilterFunction.contains(term.getName(), value))
						.collect(Collectors.toSet());
				return !filteringPassed.isEmpty() && filteringPassed.contains(true);
			};

	private static FilterFunction<Allele, String> variantTypeFilter =
			(allele, value) ->
					FilterFunction.fullMatchMultiValueOR(allele.getVariants()!=null && allele.getVariants().size()>0?allele.getVariants().stream()
							.filter(Objects::nonNull)
							.filter(variant -> variant.getVariantType() != null)
							.filter(variant->variant.getVariantType().getName()!=null && !variant.getVariantType().getName().equals(""))
							.map(variant -> variant.getVariantType().getName())
							.collect(Collectors.toSet()):new HashSet<>(), value);

	private static FilterFunction<Allele, String> alleleHasPhenotypeFilter =
			(allele, value) ->
					FilterFunction.fullMatchMultiValueOR(allele.hasPhenotype().toString(), value);

	private static FilterFunction<Allele, String> alleleHasDiseaseFilter =
			(allele, value) ->
					FilterFunction.fullMatchMultiValueOR(allele.hasDisease().toString(), value);


	private static FilterFunction<Allele, String> molecularConsequenceFilter =
			(allele, value) -> {

			  if(  allele.getVariants()==null || allele.getVariants().size()==0)
				  return false;
			  else {
			   Set<String> molecularConsequences=  allele.getVariants().stream()
						  .map(v->v.getTranscriptLevelConsequence())
						  .flatMap(Collection::stream)
						  .map(t->t.getMolecularConsequences())
						  .flatMap(List::stream)
						  .collect(Collectors.toSet());
				  return FilterFunction.fullMatchMultiValueOR(molecularConsequences.size()>0?molecularConsequences:new HashSet<>(), value);

			  }

			};

	private static FilterFunction<Allele, String> transgenicAlleleConstructFilter =
			(allele, value) ->
					FilterFunction.contains(allele.getConstructs().stream()
							.filter(Objects::nonNull)
							.map(Construct::getNameText)
							.collect(Collectors.joining()), value);

	private static FilterFunction<Allele, String> transgenicAlleleConstructRegulatedFilter =
			(allele, value) ->
			{
				String genes = allele.getConstructs().stream()
						.filter(Objects::nonNull)
						.map(Construct::getRegulatedByGenes)
						.filter(Objects::nonNull)
						.flatMap(Collection::stream)
						.map(GeneticEntity::getSymbol)
						.collect(Collectors.joining());
				String nonBGICC = allele.getConstructs().stream()
						.filter(Objects::nonNull)
						.map(Construct::getNonBGIConstructComponentsRegulation)
						.filter(Objects::nonNull)
						.flatMap(Collection::stream)
						.map(NonBGIConstructComponent::getPrimaryKey)
						.collect(Collectors.joining());
				genes += nonBGICC;
				return FilterFunction.contains(genes, value);
			};

	private static FilterFunction<Allele, String> transgenicAlleleConstructTargetedFilter =
			(allele, value) ->
			{
				String genes = allele.getConstructs().stream()
						.filter(Objects::nonNull)
						.map(Construct::getTargetGenes)
						.filter(Objects::nonNull)
						.flatMap(Collection::stream)
						.map(GeneticEntity::getSymbol)
						.collect(Collectors.joining());
				String nonBGICC = allele.getConstructs().stream()
						.filter(Objects::nonNull)
						.map(Construct::getNonBGIConstructComponentsTarget)
						.filter(Objects::nonNull)
						.flatMap(Collection::stream)
						.map(NonBGIConstructComponent::getPrimaryKey)
						.collect(Collectors.joining());
				genes += nonBGICC;
				return FilterFunction.contains(genes, value);
			};

	private static FilterFunction<Allele, String> transgenicAlleleConstructExpressedFilter =
			(allele, value) ->
			{
				String genes = allele.getConstructs().stream()
						.filter(Objects::nonNull)
						.map(Construct::getExpressedGenes)
						.filter(Objects::nonNull)
						.flatMap(Collection::stream)
						.map(GeneticEntity::getSymbol)
						.collect(Collectors.joining());
				String nonBGICC = allele.getConstructs().stream()
						.filter(Objects::nonNull)
						.map(Construct::getNonBGIConstructComponents)
						.filter(Objects::nonNull)
						.flatMap(Collection::stream)
						.map(NonBGIConstructComponent::getPrimaryKey)
						.collect(Collectors.joining());
				genes += nonBGICC;
				return FilterFunction.contains(genes, value);
			};

	private static FilterFunction<Allele, String> phenotypeFilter =
			(allele, value) -> {
				Set<Boolean> filteringPassed = allele.getPhenotypes().stream()
						.map(term -> FilterFunction.contains(term.getPhenotypeStatement(), value))
						.collect(Collectors.toSet());
				return !filteringPassed.isEmpty() && filteringPassed.contains(true);
			};

	public AlleleFiltering() {
		filterFieldMap.put(FieldFilter.SYMBOL, alleleFilter);
		filterFieldMap.put(FieldFilter.SPECIES, speciesFilter);
		filterFieldMap.put(FieldFilter.SYNONYMS, synonymFilter);
		filterFieldMap.put(FieldFilter.PHENOTYPE, phenotypeFilter);
		filterFieldMap.put(FieldFilter.DISEASE, diseaseFilter);
		filterFieldMap.put(FieldFilter.VARIANT_TYPE, variantTypeFilter);
		filterFieldMap.put(FieldFilter.MOLECULAR_CONSEQUENCE, molecularConsequenceFilter);
		filterFieldMap.put(FieldFilter.CONSTRUCT_SYMBOL, transgenicAlleleConstructFilter);
		filterFieldMap.put(FieldFilter.CONSTRUCT_REGULATED_GENE, transgenicAlleleConstructRegulatedFilter);
		filterFieldMap.put(FieldFilter.CONSTRUCT_TARGETED_GENE, transgenicAlleleConstructTargetedFilter);
		filterFieldMap.put(FieldFilter.CONSTRUCT_EXPRESSED_GENE, transgenicAlleleConstructExpressedFilter);
		filterFieldMap.put(FieldFilter.TRANSGENE_HAS_PHENOTYPE, transgenicAllelePhenotypeFilter);
		filterFieldMap.put(FieldFilter.TRANSGENE_HAS_DISEASE, transgenicAlleleDiseaseFilter);
		filterFieldMap.put(FieldFilter.HAS_PHENOTYPE, alleleHasPhenotypeFilter);
		filterFieldMap.put(FieldFilter.HAS_DISEASE, alleleHasDiseaseFilter);
		filterFieldMap.put(FieldFilter.ALLELE_CATEGORY, alleleCategoryFilter);
	}

}

