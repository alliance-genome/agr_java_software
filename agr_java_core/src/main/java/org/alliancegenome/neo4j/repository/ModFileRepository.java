package org.alliancegenome.neo4j.repository;

import org.alliancegenome.neo4j.entity.node.ModFileMetadata;

import jakarta.enterprise.context.RequestScoped;

@RequestScoped
public class ModFileRepository extends Neo4jRepository<ModFileMetadata> {

	public ModFileRepository() {
		super(ModFileMetadata.class);
	}

}
