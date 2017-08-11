package org.alliancegenome.indexer.entity;

import org.neo4j.ogm.annotation.NodeEntity;

import lombok.Data;

@NodeEntity(label="externalId")
@Data
public class ExternalId extends Identifier {

	private String name;
}
