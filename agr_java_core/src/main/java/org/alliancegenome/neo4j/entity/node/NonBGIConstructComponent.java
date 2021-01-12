package org.alliancegenome.neo4j.entity.node;

import org.alliancegenome.neo4j.entity.Neo4jEntity;
import org.neo4j.ogm.annotation.NodeEntity;

import lombok.Getter;
import lombok.Setter;

@NodeEntity
@Getter
@Setter
//@Schema(name = "Gene", description = "POJO that represents a non- BGI Construct Component")
public class NonBGIConstructComponent extends Neo4jEntity {

    public String primaryKey;
}
