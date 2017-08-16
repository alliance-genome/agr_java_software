package org.alliancegenome.indexer.entity;

import org.neo4j.ogm.annotation.NodeEntity;

import lombok.Getter;
import lombok.Setter;

@NodeEntity
@Getter @Setter
public class ExternalId extends Identifier {

	private String name;
}
