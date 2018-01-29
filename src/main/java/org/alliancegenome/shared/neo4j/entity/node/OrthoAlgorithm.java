package org.alliancegenome.shared.neo4j.entity.node;

import org.alliancegenome.shared.neo4j.entity.Neo4jEntity;
import org.neo4j.ogm.annotation.NodeEntity;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@NodeEntity
public class OrthoAlgorithm extends Neo4jEntity {

	private String name;
}
