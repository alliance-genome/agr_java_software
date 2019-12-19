package org.alliancegenome.core.service;

import org.alliancegenome.es.model.query.FieldFilter;
import org.alliancegenome.neo4j.view.OrthologView;
import org.apache.commons.collections.CollectionUtils;

import static org.alliancegenome.neo4j.view.OrthologyFilter.Stringency;

public class OrthologyFiltering extends AnnotationFiltering<OrthologView> {


    public FilterFunction<OrthologView, String> stringencyFilter =
            (orthologView, value) -> {
                Stringency stringency = Stringency.getOrthologyFilter(value);
                if (stringency == null)
                    return false;
                if (stringency.equals(Stringency.STRINGENT))
                    return FilterFunction.contains(orthologView.getStringencyFilter(), value);
                if (stringency.equals(Stringency.MODERATE))
                    return FilterFunction.contains(orthologView.getStringencyFilter(), value) ||
                            FilterFunction.contains(orthologView.getStringencyFilter(), Stringency.STRINGENT.name());
                if (stringency.equals(Stringency.ALL))
                    return true;
                return false;
            };

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

