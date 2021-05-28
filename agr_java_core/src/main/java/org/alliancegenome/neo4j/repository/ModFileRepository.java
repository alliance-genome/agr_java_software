package org.alliancegenome.neo4j.repository;

import javax.enterprise.context.RequestScoped;

import org.alliancegenome.neo4j.entity.node.ModFileMetadata;

@RequestScoped
public class ModFileRepository extends Neo4jRepository<ModFileMetadata> {

    public ModFileRepository() {
        super(ModFileMetadata.class);
    }

}
