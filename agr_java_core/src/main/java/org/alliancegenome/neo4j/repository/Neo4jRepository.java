package org.alliancegenome.neo4j.repository;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.es.model.query.FieldFilter;
import org.alliancegenome.es.util.ProcessDisplayHelper;
import org.alliancegenome.neo4j.view.BaseFilter;
import org.apache.commons.lang3.StringUtils;
import org.neo4j.ogm.config.Configuration;
import org.neo4j.ogm.cypher.ComparisonOperator;
import org.neo4j.ogm.cypher.Filter;
import org.neo4j.ogm.cypher.query.Pagination;
import org.neo4j.ogm.model.Result;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public class Neo4jRepository<E> {

	protected Class<E> entityTypeClazz;

	private SessionFactory sessionFactory;
	private Session neo4jSession;
	
	public Neo4jRepository(Class<E> entityTypeClazz) {
		this.entityTypeClazz = entityTypeClazz;
		log.info("Creating Repo for: " + entityTypeClazz);
		Configuration configuration = new Configuration.Builder().uri("bolt://" + ConfigHelper.getNeo4jHost() + ":" + ConfigHelper.getNeo4jPort()).build();
		sessionFactory = new SessionFactory(configuration, "org.alliancegenome.neo4j.entity");
		neo4jSession = sessionFactory.openSession();
		log.info("------------------------------------- Starting sessionFactory: " + getClass() + " ----------------------------- ");
		log.info("THIS NEEDS TO BE CLOSED WHEN FINISHED USE");
	}

	protected Iterable<E> getPage(int pageNumber, int pageSize, int depth) {
		Pagination p = new Pagination(pageNumber, pageSize);
		return neo4jSession.loadAll(entityTypeClazz, p, depth);
	}

	protected Iterable<E> getPage(int pageNumber, int pageSize) {
		return getPage(pageNumber, pageSize, 1);
	}

	protected int getCount() {
		return (int) neo4jSession.countEntitiesOfType(entityTypeClazz);
	}

	public void clearCache() {
		neo4jSession.clear();
	}
	
	public void close() {
		log.info("------------------------------------- Closing sessionFactory: " + getClass() + " ----------------------------- ");
		sessionFactory.close();
	}

	protected Iterable<E> getEntity(String key, String value) {
		return neo4jSession.loadAll(entityTypeClazz, new Filter(key, ComparisonOperator.EQUALS, value));
	}

	protected E getSingleEntity(String primaryKey) {
		return neo4jSession.load(entityTypeClazz, primaryKey);
	}
	
	public Iterable<E> getAll() {
		return neo4jSession.loadAll(entityTypeClazz);
	}

	protected Iterable<E> query(String cypherQuery) {
		return loggedQueryByClass(entityTypeClazz, cypherQuery, Collections.EMPTY_MAP);
	}
	protected Iterable<E> query(String cypherQuery, Map<String, ?> params) {
		return loggedQueryByClass(entityTypeClazz, cypherQuery, params);
	}
	protected <T> Iterable<T> query(Class<T> entityTypeClazz, String cypherQuery) {
		return loggedQueryByClass(entityTypeClazz, cypherQuery, Collections.EMPTY_MAP);
	}
	protected <T> Iterable<T> query(Class<T> entityTypeClazz, String cypherQuery, Map<String, ?> params) {
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
	
	protected Long queryCount(String cypherQuery) {
		return (Long) loggedQuery(cypherQuery, Collections.EMPTY_MAP).iterator().next().values().iterator().next();
	}
	protected Result queryForResult(String cypherQuery) {
		return loggedQuery(cypherQuery, Collections.EMPTY_MAP);
	}
	protected Result queryForResult(String cypherQuery, Map<String, ?> params) {
		return loggedQuery(cypherQuery, params);
	}
	protected Result loggedQuery(String cypherQuery, Map<String, ?> params) {
		Date start = new Date();
		log.debug("Running Query: " + cypherQuery);
		Result ret = neo4jSession.query(cypherQuery, params);
		Date end = new Date();
		log.debug("Query took: " + ProcessDisplayHelper.getHumanReadableTimeDisplay(end.getTime() - start.getTime()) + " to run");
		return ret;
	}


	//used by indexer repositories, assumes no params and aliased id and value fields
	protected Map<String,Set<String>> getMapSetForQuery(String query) {
		return getMapSetForQuery(query, "id", "value", null);
	}

	//used by indexer repositories, assumes aliased id and value fields but accepts params
	protected Map<String, Set<String>> getMapSetForQuery(String query, Map<String, String> params) {
		return getMapSetForQuery(query, "id", "value", params);
	}
	
	protected Map<String, Set<String>> getMapSetForQuery(String query, String keyField, String returnField) {
		return getMapSetForQuery(query, keyField, returnField, null);
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

			if (resultMap.get(returnField) instanceof String) {
				String value = (String) resultMap.get(returnField);

				returnMap.computeIfAbsent(key, x -> new HashSet<>());
				returnMap.get(key).add(value);
			} else if (resultMap.get(returnField) instanceof String[]) {
				String[] values = (String[]) resultMap.get(returnField);
				returnMap.computeIfAbsent(key, x -> new HashSet<>());
				returnMap.get(key).addAll(Arrays.stream(values).filter(x -> StringUtils.isNotEmpty(x)).collect(Collectors.toSet()));
			}


		}

		log.info(returnMap.size() + " map entries");

		return returnMap;
	}

	protected Map<String, Set<Map<String,String>>> getMapOfMapsForQuery(String query) {
		Map<String, Set<Map<String, String>>> returnMap = new HashMap<>();

		Result r;

		r = queryForResult(query);

		Iterator<Map<String, Object>> i = r.iterator();

		while (i.hasNext()) {
			Map<String, String> rowMap = new HashMap<>();
			Map<String, Object> resultMap = i.next();
			String key = (String) resultMap.get("id");

			//make a map of selected columns
			for (String rowName : resultMap.keySet()) {
				if (!rowName.equals("id")) {
					rowMap.put(rowName, (String) resultMap.get(rowName));
				}
			}

			returnMap.computeIfAbsent(key, x -> new HashSet<>());
			returnMap.get(key).add(rowMap);
		}

		log.info(returnMap.size() + " map entries");

		return returnMap;
	}

	protected String addAndWhereClauseString(String fieldName, FieldFilter fieldFilter, BaseFilter baseFilter) {
		return addWhereClauseString(fieldName, fieldFilter, baseFilter, " AND ");
	}


	protected String addWhereClauseString(String fieldName, FieldFilter fieldFilter, BaseFilter baseFilter, String connectorLogic) {
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
