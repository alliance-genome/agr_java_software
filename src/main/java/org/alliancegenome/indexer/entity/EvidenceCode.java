package org.alliancegenome.indexer.entity;

import lombok.Getter;
import lombok.Setter;
import org.neo4j.ogm.annotation.NodeEntity;

@NodeEntity
@Getter
@Setter
public class EvidenceCode extends Neo4jNode {
    private String primaryKey;
}
