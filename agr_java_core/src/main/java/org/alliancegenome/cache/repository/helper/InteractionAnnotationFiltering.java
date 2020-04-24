package org.alliancegenome.cache.repository.helper;

import java.util.HashMap;
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

    private static FilterFunction<InteractionGeneJoin, String> interactorGeneSymbolFilter =
            (annotation, value) -> FilterFunction.contains(annotation.getGeneB().getSymbol(), value);

    private static FilterFunction<InteractionGeneJoin, String> speciesFilter =
            (annotation, value) -> FilterFunction.fullMatchMultiValueOR(annotation.getGeneB().getSpecies().getName(), value);

    private static FilterFunction<InteractionGeneJoin, String> referenceFilter =
            (annotation, value) -> FilterFunction.contains(annotation.getPublication().getPubId(), value);

    public static FilterFunction<InteractionGeneJoin, String> sourceFilter =
            (annotation, value) -> {
                Set<Boolean> filteringPassed = annotation.getCrossReferences().stream()
                        .map(referenceName -> {
                            String entityName = referenceName.getPrefix() + ":" + referenceName.getDisplayName();
                            return FilterFunction.contains(entityName, value);
                        })
                        .collect(Collectors.toSet());
                
                String dbNames = 
                        annotation.getSourceDatabase().getLabel() + " " + 
                        annotation.getSourceDatabase().getDisplayName() + " " +
                        annotation.getAggregationDatabase().getLabel() + " " + 
                        annotation.getAggregationDatabase().getDisplayName();
                
                filteringPassed.add(FilterFunction.contains(dbNames, value));
                
                // return true if at least one source is found in either crossreferences or source and agg db's
                return filteringPassed.contains(true);
            };

    public static FilterFunction<InteractionGeneJoin, String> detectionMethodFilter =
            (annotation, value) -> {
                Set<Boolean> filteringPassed = annotation.getDetectionsMethods().stream()
                        .map(methodName -> FilterFunction.contains(methodName.getLabel(), value))
                        .collect(Collectors.toSet());
                // return true if at least one source is found
                return filteringPassed.contains(true);
            };


    public static Map<FieldFilter, FilterFunction<InteractionGeneJoin, String>> filterFieldMap = new HashMap<>();

    static {
        filterFieldMap.put(FieldFilter.INTERACTOR_GENE_SYMBOL, interactorGeneSymbolFilter);
        filterFieldMap.put(FieldFilter.INTERACTOR_SPECIES, speciesFilter);
        filterFieldMap.put(FieldFilter.MOLECULE_TYPE, moleculeTypeFilter);
        filterFieldMap.put(FieldFilter.INTERACTOR_MOLECULE_TYPE, interactorMoleculeTypeFilter);
        filterFieldMap.put(FieldFilter.DETECTION_METHOD, detectionMethodFilter);
        filterFieldMap.put(FieldFilter.FREFERENCE, referenceFilter);
        filterFieldMap.put(FieldFilter.SOURCE, sourceFilter);
    }

}

