package org.alliancegenome.api.service;

import org.apache.commons.lang3.StringUtils;

import javax.enterprise.context.RequestScoped;
import java.util.regex.Pattern;

@RequestScoped
public class QueryManipulationService {

    private static final String ESCAPE_CHARS = "[\\<\\>\\/]";
    private static final Pattern LUCENE_PATTERN = Pattern.compile(ESCAPE_CHARS);
    private static final String REPLACEMENT_STRING = "\\\\$0";


    public String processQuery(String query) {
        query = luceneEscape(query);
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

    private String luceneEscape(String value) {
        if (StringUtils.isEmpty(value)) {
            return value;
        }
        String escaped = LUCENE_PATTERN.matcher(value).replaceAll(REPLACEMENT_STRING);
        return escaped;
    }

}
