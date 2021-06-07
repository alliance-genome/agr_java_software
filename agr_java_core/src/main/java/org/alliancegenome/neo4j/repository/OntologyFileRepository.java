package org.alliancegenome.neo4j.repository;

import javax.enterprise.context.RequestScoped;

import org.alliancegenome.neo4j.entity.node.OntologyFileMetadata;

@RequestScoped
public class OntologyFileRepository extends Neo4jRepository<OntologyFileMetadata> {

    public OntologyFileRepository() {
        super(OntologyFileMetadata.class);
    }

}