package org.alliancegenome.neo4j.repository;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.alliancegenome.es.model.query.FieldFilter;
import org.alliancegenome.es.util.ProcessDisplayHelper;
import org.alliancegenome.neo4j.view.BaseFilter;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.neo4j.ogm.cypher.ComparisonOperator;
import org.neo4j.ogm.cypher.Filter;
import org.neo4j.ogm.cypher.query.Pagination;
import org.neo4j.ogm.model.Result;
import org.neo4j.ogm.session.Session;

@SuppressWarnings("unchecked")
public class Neo4jRepository<E> {

    private final Logger log = LogManager.getLogger(getClass());

    protected Class<E> entityTypeClazz;
    private Session neo4jSession = Neo4jSessionFactory.getInstance().getNeo4jSession();

    public Neo4jRepository(Class<E> entityTypeClazz) {
        this.entityTypeClazz = entityTypeClazz;
    }

    public Iterable<E> getPage(int pageNumber, int pageSize, int depth) {
        Pagination p = new Pagination(pageNumber, pageSize);
        return neo4jSession.loadAll(entityTypeClazz, p, depth);
    }

    public Iterable<E> getPage(int pageNumber, int pageSize) {
        return getPage(pageNumber, pageSize, 1);
    }

    public int getCount() {
        return (int) neo4jSession.countEntitiesOfType(entityTypeClazz);
    }

    public void clearCache() {
        neo4jSession.clear();
    }

    public Iterable<E> getEntity(String key, String value) {
        return neo4jSession.loadAll(entityTypeClazz, new Filter(key, ComparisonOperator.EQUALS, value));
    }

    public E getSingleEntity(String primaryKey) {
        return neo4jSession.load(entityTypeClazz, primaryKey);
    }
    

    public Iterable<E> query(String cypherQuery) {
        return loggedQueryByClass(entityTypeClazz, cypherQuery, Collections.EMPTY_MAP);
    }
    public Iterable<E> query(String cypherQuery, Map<String, ?> params) {
        return loggedQueryByClass(entityTypeClazz, cypherQuery, params);
    }
    public <T> Iterable<T> query(Class<T> entityTypeClazz, String cypherQuery) {
        return loggedQueryByClass(entityTypeClazz, cypherQuery, Collections.EMPTY_MAP);
    }
    public <T> Iterable<T> query(Class<T> entityTypeClazz, String cypherQuery, Map<String, ?> params) {
        return loggedQueryByClass(entityTypeClazz, cypherQuery, params);
    }
    private <T> Iterable<T> loggedQueryByClass(Class<T> entityTypeClazz, String cypherQuery, Map<String, ?> params) {
        Date start = new Date();
        log.debug("Running Query: " + cypherQuery);
        Iterable<T> ret = neo4jSession.query(entityTypeClazz, cypherQuery, params);
        Date end = new Date();
        log.debug("Query took: " + ProcessDisplayHelper.getHumanReadableTimeDisplay(end.getTime() - start.getTime()) + " to run");
        return ret;
    }
    
    public Long queryCount(String cypherQuery) {
        return (Long) loggedQuery(cypherQuery, Collections.EMPTY_MAP).iterator().next().values().iterator().next();
    }
    public Result queryForResult(String cypherQuery) {
        return loggedQuery(cypherQuery, Collections.EMPTY_MAP);
    }
    public Result queryForResult(String cypherQuery, Map<String, ?> params) {
        return loggedQuery(cypherQuery, params);
    }
    private Result loggedQuery(String cypherQuery, Map<String, ?> params) {
        Date start = new Date();
        log.debug("Running Query: " + cypherQuery);
        Result ret = neo4jSession.query(cypherQuery, params);
        Date end = new Date();
        log.debug("Query took: " + ProcessDisplayHelper.getHumanReadableTimeDisplay(end.getTime() - start.getTime()) + " to run");
        return ret;
    }
    
    
    

    //used by Gene & Allele indexer repositories
    protected String getSpeciesWhere(String species) {
        if (StringUtils.isNotEmpty(species)) {
            return " WHERE species.name = {species} ";
        }
        return "";
    }

    //used by Gene & Allele indexer repositories
    protected Map<String, String> getSpeciesParams(String species) {
        Map<String, String> params = null;
        if (StringUtils.isNotEmpty(species)) {
            params = new HashMap<String, String>() {{
                put("species", species);
            }};
        }
        return params;
    }

    //used by indexer repositories, assumes no params and aliased id and value fields
    protected Map<String,Set<String>> getMapSetForQuery(String query) {
        return getMapSetForQuery(query, "id", "value", null);
    }

    //used by indexer repositories
    protected Map<String, Set<String>> getMapSetForQuery(String query, String keyField, String returnField, Map<String, String> params) {

        Map<String, Set<String>> returnMap = new HashMap<>();

        Result r;

        if (params == null) {
            r = queryForResult(query);
        } else {
            r = queryForResult(query, params);
        }

        Iterator<Map<String, Object>> i = r.iterator();

        while (i.hasNext()) {
            Map<String, Object> resultMap = i.next();
            String key = (String) resultMap.get(keyField);
            String value = (String) resultMap.get(returnField);

            returnMap.computeIfAbsent(key, x -> new HashSet<>());
            returnMap.get(key).add(value);
        }

        log.info(returnMap.size() + " map entries");

        return returnMap;
    }

    String addAndWhereClauseString(String fieldName, FieldFilter fieldFilter, BaseFilter baseFilter) {
        return addWhereClauseString(fieldName, fieldFilter, baseFilter, " AND ");
    }


    String addWhereClauseString(String fieldName, FieldFilter fieldFilter, BaseFilter baseFilter, String connectorLogic) {
        String value = baseFilter.get(fieldFilter);
        String query = null;
        if (value != null) {
            query = "";
            if (connectorLogic != null) {
                query = connectorLogic;
            }
            query += " LOWER(" + fieldName + ") =~ '.*" + value.toLowerCase() + ".*' ";
        }
        return query;
    }


}
