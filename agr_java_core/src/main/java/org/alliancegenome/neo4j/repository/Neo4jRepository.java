package org.alliancegenome.neo4j.repository;

import java.util.*;

import org.alliancegenome.es.model.query.FieldFilter;
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
    protected Session neo4jSession = Neo4jSessionFactory.getInstance().getNeo4jSession();

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

    public Long queryCount(String cypherQuery) {
        return (Long) neo4jSession.query(cypherQuery, Collections.EMPTY_MAP).iterator().next().values().iterator().next();
    }

    public Iterable<E> query(String cypherQuery) {
        return neo4jSession.query(entityTypeClazz, cypherQuery, Collections.EMPTY_MAP);
    }

    public Iterable<E> query(String cypherQuery, Map<String, ?> params) {
        return neo4jSession.query(entityTypeClazz, cypherQuery, params);
    }

    public Result queryForResult(String cypherQuery) {
        return neo4jSession.query(cypherQuery, Collections.EMPTY_MAP);
    }

    public Result queryForResult(String cypherQuery, Map<String, ?> params) {
        return neo4jSession.query(cypherQuery, params);
    }

    //used by Gene & Feature indexer repositories
    protected String getSpeciesWhere(String species) {
        if (StringUtils.isNotEmpty(species)) {
            return " WHERE species.name = {species} ";
        }
        return "";
    }

    //used by Gene & Feature indexer repositories
    protected Map<String, String> getSpeciesParams(String species) {
        Map<String, String> params = null;
        if (StringUtils.isNotEmpty(species)) {
            params = new HashMap<String, String>() {{
                put("species", species);
            }};
        }
        return params;
    }

    //used by indexer repositories
    protected Map<String, Set<String>> getMapSetForQuery(String query, String keyField,
                                                         String returnField, Map<String, String> params) {

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
