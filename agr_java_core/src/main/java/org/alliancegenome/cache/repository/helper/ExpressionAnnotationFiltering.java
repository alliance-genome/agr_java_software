package org.alliancegenome.cache.repository.helper;

import java.util.*;
import java.util.stream.Collectors;

import org.alliancegenome.core.ExpressionDetail;
import org.alliancegenome.es.model.query.FieldFilter;

public class ExpressionAnnotationFiltering extends AnnotationFiltering {

    private static FilterFunction<ExpressionDetail, String> speciesFilter =
            (annotation, value) -> FilterFunction.fullMatchMultiValueOR(annotation.getGene().getSpecies().getName(), value);

    private static FilterFunction<ExpressionDetail, String> geneNameFilter =
            (annotation, value) -> FilterFunction.contains(annotation.getGene().getSymbol(), value);

    private static FilterFunction<ExpressionDetail, String> filterTermFilter =
            (annotation, value) -> FilterFunction.contains(annotation.getTermName(), value);

    private static FilterFunction<ExpressionDetail, String> assayFilter =
            (annotation, value) -> FilterFunction.contains(annotation.getAssay().getDisplaySynonym(), value);

    private static FilterFunction<ExpressionDetail, String> stageFilter =
            (annotation, value) -> FilterFunction.contains(annotation.getStage().getName(), value);

    private static FilterFunction<ExpressionDetail, String> sourceFilter =
            (annotation, value) -> {
                if(annotation.getCrossReferences() == null)
                    return false;
                Set<Boolean> filteringPassed = annotation.getCrossReferences().stream()
                        .map(crossReference -> FilterFunction.contains(crossReference.getDisplayName(), value))
                        .collect(Collectors.toSet());
                // return true if at least one source is found
                return filteringPassed.contains(true);
            };

    public static FilterFunction<ExpressionDetail, String> referenceFilter =
            (annotation, value) -> {
                Set<Boolean> filteringPassed = annotation.getPublications().stream()
                        .map(referenceName -> FilterFunction.contains(referenceName.getPubId(), value))
                        .collect(Collectors.toSet());
                // return true if at least one source is found
                return filteringPassed.contains(true);
            };


    public static Map<FieldFilter, FilterFunction<ExpressionDetail, String>> filterFieldMap = new HashMap<>();

    static {
        filterFieldMap.put(FieldFilter.FSPECIES, speciesFilter);
        filterFieldMap.put(FieldFilter.GENE_NAME, geneNameFilter);
        filterFieldMap.put(FieldFilter.TERM_NAME, filterTermFilter);
        filterFieldMap.put(FieldFilter.ASSAY, assayFilter);
        filterFieldMap.put(FieldFilter.STAGE, stageFilter);
        filterFieldMap.put(FieldFilter.FREFERENCE, referenceFilter);
        filterFieldMap.put(FieldFilter.SOURCE, sourceFilter);
    }

}

