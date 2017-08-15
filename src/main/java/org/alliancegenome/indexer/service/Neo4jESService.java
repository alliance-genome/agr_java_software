package org.alliancegenome.indexer.service;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.alliancegenome.indexer.config.ConfigHelper;
import org.alliancegenome.indexer.util.Neo4jSessionFactory;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.neo4j.ogm.cypher.query.Pagination;
import org.neo4j.ogm.session.Session;

public class Neo4jESService<E> {

	protected Class<E> entityTypeClazz;
	protected Session neo4jSession;
	protected PreBuiltTransportClient esSearchClient = new PreBuiltTransportClient(Settings.EMPTY);

	public Neo4jESService(Class<E> entityTypeClazz) {
		try {
			//entityType = entityType.
			this.entityTypeClazz = entityTypeClazz;
			neo4jSession = Neo4jSessionFactory.getInstance().getNeo4jSession();
			esSearchClient.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(ConfigHelper.getEsHost()), ConfigHelper.getEsPort()));
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}
	
	public Iterable<E> getPage(int pageNumber, int pageSize, int depth) {
		Pagination p = new Pagination(pageNumber, pageSize);
		return neo4jSession.loadAll(entityTypeClazz, p, depth);
	}
	
	public Iterable<E> getPage(int pageNumber, int pageSize) {
		return getPage(pageNumber, pageSize, 1);
	}
	
	public int getCount() {
		return (int)neo4jSession.countEntitiesOfType(entityTypeClazz);
	}
	
//	public Iterable<E> findAll() {
//		return neo4jSession.loadAll(entityTypeClazz);
//		//loadAll(entityTypeClazz, DEPTH_LIST);
//	}
//
//	public E find(Long id) {
//		return neo4jSession.load(entityTypeClazz, id);
//	}

}