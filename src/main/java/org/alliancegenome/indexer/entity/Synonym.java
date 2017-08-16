package org.alliancegenome.indexer.entity;

import org.neo4j.ogm.annotation.NodeEntity;

import lombok.Data;

@NodeEntity
@Data
public class Synonym extends Identifier {

	private String primaryKey;
	private String name;
}
