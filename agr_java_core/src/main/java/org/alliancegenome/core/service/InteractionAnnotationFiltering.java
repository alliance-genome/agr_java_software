package org.alliancegenome.core.service;

import org.alliancegenome.es.model.query.FieldFilter;
import org.alliancegenome.neo4j.entity.node.InteractionGeneJoin;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class InteractionAnnotationFiltering extends AnnotationFiltering {


    private static FilterFunction<InteractionGeneJoin, String> interactorMoleculeTypeFilterA =
            (annotation, value) -> FilterFunction.contains(annotation.getInteractorAType().getLabel(), value);

    private static FilterFunction<InteractionGeneJoin, String> interactorMoleculeTypeFilterB =
            (annotation, value) -> FilterFunction.contains(annotation.getInteractorBType().getLabel(), value);

    private static FilterFunction<InteractionGeneJoin, String> interactorGeneSymbolFilterB =
            (annotation, value) -> FilterFunction.contains(annotation.getGeneB().getSymbol(), value);

    private static FilterFunction<InteractionGeneJoin, String> interactorGeneSymbolFilterA =
            (annotation, value) -> FilterFunction.contains(annotation.getGeneA().getSymbol(), value);

    private static FilterFunction<InteractionGeneJoin, String> speciesFilterB =
            (annotation, value) -> FilterFunction.contains(annotation.getGeneB().getSpecies().getName(), value);

    private static FilterFunction<InteractionGeneJoin, String> speciesFilterA =
            (annotation, value) -> FilterFunction.contains(annotation.getGeneA().getSpecies().getName(), value);

    private static FilterFunction<InteractionGeneJoin, String> referenceFilter =
            (annotation, value) -> FilterFunction.contains(annotation.getPublication().getPubId(), value);

    public static FilterFunction<InteractionGeneJoin, String> sourceFilter =
            (annotation, value) -> {
                Set<Boolean> filteringPassed = annotation.getCrossReferences().stream()
                        .map(referenceName -> FilterFunction.contains(referenceName.getName(), value))
                        .collect(Collectors.toSet());
                // return true if at least one source is found
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
        filterFieldMap.put(FieldFilter.INTERACTOR_GENE_SYMBOL_A, interactorGeneSymbolFilterA);
        filterFieldMap.put(FieldFilter.INTERACTOR_GENE_SYMBOL_B, interactorGeneSymbolFilterB);
        filterFieldMap.put(FieldFilter.INTERACTOR_SPECIES_A, speciesFilterA);
        filterFieldMap.put(FieldFilter.INTERACTOR_SPECIES_B, speciesFilterB);
        filterFieldMap.put(FieldFilter.MOLECULE_TYPE_A, interactorMoleculeTypeFilterA);
        filterFieldMap.put(FieldFilter.MOLECULE_TYPE_B, interactorMoleculeTypeFilterB);
        filterFieldMap.put(FieldFilter.DETECTION_METHOD, detectionMethodFilter);
        filterFieldMap.put(FieldFilter.FREFERENCE, referenceFilter);
        filterFieldMap.put(FieldFilter.SOURCE, sourceFilter);
    }

}

