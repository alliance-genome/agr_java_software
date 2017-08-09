package org.alliancegenome.indexer.entity;

import org.neo4j.ogm.annotation.GraphId;
import org.neo4j.ogm.annotation.NodeEntity;

import lombok.Data;

@NodeEntity
@Data
public class Synonym extends Entity {
	@GraphId
	private Long id;
	private String primaryKey;
}
