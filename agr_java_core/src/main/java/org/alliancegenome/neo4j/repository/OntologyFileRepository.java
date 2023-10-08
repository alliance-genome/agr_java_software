package org.alliancegenome.neo4j.repository;

import org.alliancegenome.neo4j.entity.node.OntologyFileMetadata;

import jakarta.enterprise.context.RequestScoped;

@RequestScoped
public class OntologyFileRepository extends Neo4jRepository<OntologyFileMetadata> {

	public OntologyFileRepository() {
		super(OntologyFileMetadata.class);
	}

}