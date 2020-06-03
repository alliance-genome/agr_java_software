package org.alliancegenome.api.service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

public class QueryManipulationService {

    private static final String ESCAPE_CHARS = "[/\\[\\](){}]";
    private static final Pattern LUCENE_PATTERN = Pattern.compile(ESCAPE_CHARS);
    private static final String REPLACEMENT_STRING = "\\\\$0";
    private static final Pattern HGVS_PATTERN = Pattern.compile("([A-Z]{2}_[0-9\\.]+[\\\\]*:g.[\\d\\w><_]+)");

    public String processQuery(String query) {
        query = luceneEscape(query);
        query = escapeColons(query);
        query = quoteHgvs(query);
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

    private String quoteHgvs(String value) {
        if (StringUtils.isEmpty(value)) {
            return value;
        }

        Matcher m = HGVS_PATTERN.matcher(value);
        while(m.find()) {
            String match = m.group(0);
            value = value.replace(match,"\"" + match + "\"");
        }
        //an already quoted term will get an extra pair, so just clean them up all at once
        value = value.replaceAll("\"\"","\"");

        return value;
    }
}
