package org.alliancegenome.indexer.service;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.alliancegenome.indexer.config.ConfigHelper;
import org.alliancegenome.indexer.util.Neo4jSessionFactory;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.neo4j.ogm.session.Session;

public class Neo4jESService<E> {

	private static final int DEPTH_LIST = 0;
	private static final int DEPTH_ENTITY = 1;
	private E entity;
	protected Session neo4jSession;
	protected PreBuiltTransportClient esSearchClient = new PreBuiltTransportClient(Settings.EMPTY);

	public Neo4jESService() {
		try {
			neo4jSession = Neo4jSessionFactory.getInstance().getNeo4jSession();
			esSearchClient.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(ConfigHelper.getEsHost()), ConfigHelper.getEsPort()));
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}

	public Iterable<E> findAll() {
		return neo4jSession.loadAll(getEntityType(), DEPTH_LIST);
	}

	public E find(Long id) {
		return neo4jSession.load(getEntityType(), id, DEPTH_ENTITY);
	}

	public Class<E> getEntityType() {
		return ((Class<E>) entity.getClass());
	}
}
