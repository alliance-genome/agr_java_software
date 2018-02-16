package org.alliancegenome.shared.neo4j.repository;

import java.util.Collections;
import java.util.Map;

import org.neo4j.ogm.cypher.ComparisonOperator;
import org.neo4j.ogm.cypher.Filter;
import org.neo4j.ogm.cypher.query.Pagination;
import org.neo4j.ogm.model.Result;
import org.neo4j.ogm.session.Session;

@SuppressWarnings("unchecked")
public class Neo4jRepository<E> {

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
		return (Long) neo4jSession.query(cypherQuery, Collections.EMPTY_MAP ).iterator().next().values().iterator().next();
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


}
