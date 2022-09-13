package org.alliancegenome.neo4j.entity;

import java.io.Serializable;

import lombok.*;

@Getter @Setter
public abstract class Neo4jEntity implements Serializable {

	private Long id;

}
