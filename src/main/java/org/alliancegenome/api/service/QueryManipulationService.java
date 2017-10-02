package org.alliancegenome.api.service;

import javax.enterprise.context.RequestScoped;

@RequestScoped
public class QueryManipulationService {

    public String processQuery(String query) {
        query = escapeColons(query);
        return query;

    }

    private String escapeColons(String query) {
        if(query == null) return null;

        //start by escaping all colons
        query = query.replaceAll(":", "\\\\:");

        //so that it's still possible to do per field queries,
        //allow for __ as a prefix for field names so that you
        //can do symbol:pax2 style searches.  (assuming field
        //names have lowercase, uppercase and underscores
        query = query.replaceAll("(__)([a-zA-Z_]+)\\\\:","$2:");
        return query;
    }
}
