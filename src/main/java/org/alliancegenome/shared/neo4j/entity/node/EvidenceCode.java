package org.alliancegenome.shared.neo4j.entity.node;

import lombok.Getter;
import lombok.Setter;

import org.alliancegenome.shared.neo4j.entity.Neo4jEntity;
import org.neo4j.ogm.annotation.NodeEntity;

@NodeEntity
@Getter
@Setter
public class EvidenceCode extends Neo4jEntity {
	private String primaryKey;
}
