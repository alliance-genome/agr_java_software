package org.alliancegenome.neo4j.view;

import lombok.Getter;
import lombok.Setter;
import org.alliancegenome.es.model.query.FieldFilter;

import java.util.HashMap;

@Setter
@Getter
public class BaseFilter extends HashMap<FieldFilter, String> {

    public void addFieldFilter(FieldFilter fieldFilter, String value) {
        put(fieldFilter, value);
    }

    public String getFilterValue(FieldFilter fieldFilter) {
        return get(fieldFilter);
    }

}
