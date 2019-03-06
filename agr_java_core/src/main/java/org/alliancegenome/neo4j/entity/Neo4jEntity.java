package org.alliancegenome.neo4j.entity;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter @Setter
public abstract class Neo4jEntity implements Serializable {

    private Long id;

}
