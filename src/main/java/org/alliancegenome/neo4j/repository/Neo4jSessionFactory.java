package org.alliancegenome.neo4j.repository;

import org.alliancegenome.core.config.ConfigHelper;
import org.neo4j.ogm.config.Configuration;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;


public class Neo4jSessionFactory {

	private static SessionFactory sessionFactory = null;
	private static Neo4jSessionFactory factory = new Neo4jSessionFactory();

	public static Neo4jSessionFactory getInstance() {
		return factory;
	}

	// prevent external instantiation
	private Neo4jSessionFactory() {
	}

	public Session getNeo4jSession() {
		if(sessionFactory == null) {
//						Builder b = new Builder();
//						b.uri("bolt://" + ConfigHelper.getNeo4jHost() + ":" + ConfigHelper.getNeo4jPort());
//						b.verifyConnection(true);
//						b.encryptionLevel("NONE");
//						sessionFactory = new SessionFactory(b.build(), "org.alliancegenome.shared.neo4j.entity");

//			Configuration configuration = new Configuration();
//			configuration.driverConfiguration().setDriverClassName("org.neo4j.ogm.drivers.bolt.driver.BoltDriver")
//			.setURI("bolt://" + ConfigHelper.getNeo4jHost() + ":" + ConfigHelper.getNeo4jPort());
//			sessionFactory = new SessionFactory(configuration, "org.alliancegenome.shared.neo4j.entity");

			Configuration configuration = new Configuration.Builder().uri("bolt://" + ConfigHelper.getNeo4jHost() + ":" + ConfigHelper.getNeo4jPort()).build();
			sessionFactory = new SessionFactory(configuration, "org.alliancegenome.neo4j.entity");

		}
		return sessionFactory.openSession();
	}
}

