package org.alliancegenome.core.service;

import org.alliancegenome.es.model.query.FieldFilter;
import org.alliancegenome.neo4j.view.OrthologView;
import org.apache.commons.collections.CollectionUtils;

public class OrthologyFiltering extends AnnotationFiltering<OrthologView> {


    public FilterFunction<OrthologView, String> stringencyFilter =
            (orthologView, value) -> FilterFunction.contains(orthologView.getStringencyFilter(), value);

    public FilterFunction<OrthologView, String> methodFilter =
            (orthologView, value) -> {
                if (CollectionUtils.isEmpty(orthologView.getPredictionMethodsMatched()))
                    return false;
                String concatenatedMethods = String.join(",", orthologView.getPredictionMethodsMatched());
                return FilterFunction.contains(concatenatedMethods, value);
            };

    public OrthologyFiltering() {
        filterFieldMap.put(FieldFilter.STRINGENCY, stringencyFilter);
        filterFieldMap.put(FieldFilter.ORTHOLOGY_METHOD, methodFilter);
    }

}

