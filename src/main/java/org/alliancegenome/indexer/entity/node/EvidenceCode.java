package org.alliancegenome.indexer.entity.node;

import lombok.Getter;
import lombok.Setter;

import org.alliancegenome.indexer.entity.Neo4jEntity;
import org.neo4j.ogm.annotation.NodeEntity;

@NodeEntity
@Getter
@Setter
public class EvidenceCode extends Neo4jEntity {
    private String primaryKey;
}
