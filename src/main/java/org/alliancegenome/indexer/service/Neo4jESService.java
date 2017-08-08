package org.alliancegenome.indexer.service;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.alliancegenome.indexer.util.ConfigHelper;
import org.alliancegenome.indexer.util.Neo4jSessionFactory;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.neo4j.ogm.session.Session;

public abstract class Neo4jESService<E, D> implements Service<E, D> {

	private static final int DEPTH_LIST = 0;
	private static final int DEPTH_ENTITY = 1;
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
	
	@Override
	public Iterable<E> findAll() {
		return neo4jSession.loadAll(getEntityType(), DEPTH_LIST);
	}

	@Override
	public E find(Long id) {
		return neo4jSession.load(getEntityType(), id, DEPTH_ENTITY);
	}

	@Override
	public void delete(Long id) {
		//session.delete(session.load(getEntityType(), id));
	}
	
	@Override
	public Iterable<D> create(Iterable<D> documents) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public D create(D document) {
		//session.save(entity, DEPTH_ENTITY);
		//return find(entity.id);
		return null;
	}

	public abstract Class<E> getEntityType();

}
