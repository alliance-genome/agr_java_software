package org.alliancegenome.cache.repository.helper;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.alliancegenome.es.model.query.FieldFilter;
import org.alliancegenome.neo4j.entity.node.InteractionGeneJoin;

public class InteractionAnnotationFiltering extends AnnotationFiltering {


	private static FilterFunction<InteractionGeneJoin, String> interactorMoleculeTypeFilter =
			(annotation, value) -> FilterFunction.fullMatchMultiValueOR(annotation.getInteractorBType().getDisplayName(), value);

	private static FilterFunction<InteractionGeneJoin, String> moleculeTypeFilter =
			(annotation, value) -> FilterFunction.fullMatchMultiValueOR(annotation.getInteractorAType().getDisplayName(), value);

	//add this to for API to filter out genetic/molecular interaction type
	private static FilterFunction<InteractionGeneJoin, String> joinTypeFilter =
					(annotation, value) -> FilterFunction.fullMatchMultiValueOR(annotation.getJoinType(), value);

	private static FilterFunction<InteractionGeneJoin, String> interactorGeneSymbolFilter =
			(annotation, value) -> FilterFunction.contains(annotation.getGeneB().getSymbol(), value);

	private static FilterFunction<InteractionGeneJoin, String> speciesFilter =
			(annotation, value) -> FilterFunction.fullMatchMultiValueOR(annotation.getGeneB().getSpecies().getName(), value);

	private static FilterFunction<InteractionGeneJoin, String> referenceFilter =
			(annotation, value) -> FilterFunction.contains(annotation.getPublication().getPubId(), value);
	//CrossReference, sourceDatabase, aggregationDatabase could be null, so need to check for null
	public static FilterFunction<InteractionGeneJoin, String> sourceFilter =
			(annotation, value) -> {
				Set<Boolean> filteringPassed = new	 HashSet<Boolean>();
				if (annotation.getCrossReferences() !=null) {
				  filteringPassed = annotation.getCrossReferences().stream()
						.map(referenceName -> {
							String entityName = referenceName.getPrefix() + ":" + referenceName.getDisplayName();
							return FilterFunction.contains(entityName, value);
						})
						.collect(Collectors.toSet());
				}
				String dbNames = "";
				if (annotation.getSourceDatabase()!=null)
					dbNames =
						annotation.getSourceDatabase().getLabel() + " " + 
						annotation.getSourceDatabase().getDisplayName() ;
				if (annotation.getAggregationDatabase()!=null) 
					dbNames += " " +
						annotation.getAggregationDatabase().getLabel() + " " + 
						annotation.getAggregationDatabase().getDisplayName();
				
				filteringPassed.add(FilterFunction.contains(dbNames, value));
				
				// return true if at least one source is found in either crossreferences or source and agg db's
				return filteringPassed.contains(true);
			};

	public static FilterFunction<InteractionGeneJoin, String> detectionMethodFilter =
			(annotation, value) -> {
				if (annotation.getDetectionsMethods()==null)
					return false;
				else {
				Set<Boolean> filteringPassed = annotation.getDetectionsMethods().stream()
						.map(methodName -> FilterFunction.contains(methodName.getLabel(), value))
						.collect(Collectors.toSet());
				// return true if at least one source is found
				return filteringPassed.contains(true);
				}
			};

			//additional filter for genetic interaction
			private static FilterFunction<InteractionGeneJoin, String> roleFilter =
			   (annotation, value) -> FilterFunction.fullMatchMultiValueOR(annotation.getInteractorARole().getDisplayName(), value); 
			   
		   private static FilterFunction<InteractionGeneJoin, String> interactorRoleFilter =
					   (annotation, value) -> FilterFunction.fullMatchMultiValueOR(annotation.getInteractorBRole().getDisplayName(), value); 
					   
		   
		   private static FilterFunction<InteractionGeneJoin, String> geneticPerturbationFilter =
							   (annotation, value) -> { 
									 if (annotation.getAlleleA()  != null)
									   return FilterFunction.contains(annotation.getAlleleA().getSymbolText(), value); 
									 else 
										 return false;
								   } ;			   
		   private static FilterFunction<InteractionGeneJoin, String> interactorGeneticPerturbationFilter =
									   (annotation, value) -> {
										  if (annotation.getAlleleB() !=null)
										   return FilterFunction.contains(annotation.getAlleleB().getSymbolText(), value); 
										  else 
											  return false;
									   };
									   
			public static FilterFunction<InteractionGeneJoin, String> phenotypesFilter =
											   (annotation, value) -> {
												   if (annotation.getPhenotypes() ==null)
													   return false;
												   else {
													 Set<Boolean> filteringPassed = annotation.getPhenotypes().stream()
														   .map(phenotype -> FilterFunction.contains(phenotype.getPhenotypeStatement(), value))
														   .collect(Collectors.toSet());
													 // return true if at least one source is found
													 return filteringPassed.contains(true);
												   }
											   };						   
			private static FilterFunction<InteractionGeneJoin, String> interactionTypeFilter =
				   (annotation, value) -> FilterFunction.fullMatchMultiValueOR(annotation.getInteractionType().getDisplayName(), value);  
	public static Map<FieldFilter, FilterFunction<InteractionGeneJoin, String>> filterFieldMap = new HashMap<>();

	static {
		filterFieldMap.put(FieldFilter.INTERACTOR_GENE_SYMBOL, interactorGeneSymbolFilter);
		filterFieldMap.put(FieldFilter.INTERACTOR_SPECIES, speciesFilter);
		filterFieldMap.put(FieldFilter.MOLECULE_TYPE, moleculeTypeFilter);
		filterFieldMap.put(FieldFilter.JOIN_TYPE, joinTypeFilter);//add for interaction/molecular interaction type
		filterFieldMap.put(FieldFilter.INTERACTOR_MOLECULE_TYPE, interactorMoleculeTypeFilter);
		filterFieldMap.put(FieldFilter.DETECTION_METHOD, detectionMethodFilter);
		filterFieldMap.put(FieldFilter.INTERACTOR_REFERENCE, referenceFilter);
		filterFieldMap.put(FieldFilter.SOURCE, sourceFilter);
		//add filter for genetic interaction
		filterFieldMap.put(FieldFilter.ROLE, roleFilter);
		filterFieldMap.put(FieldFilter.INTERACTOR_ROLE, interactorRoleFilter);
		filterFieldMap.put(FieldFilter.GENETIC_PERTURBATION, geneticPerturbationFilter);
		filterFieldMap.put(FieldFilter.INTERACTOR_GENETIC_PERTURBATION, interactorGeneticPerturbationFilter);
		filterFieldMap.put(FieldFilter.PHENOTYPES, phenotypesFilter);
		filterFieldMap.put(FieldFilter.INTERACTION_TYPE, interactionTypeFilter);		
	}

}

