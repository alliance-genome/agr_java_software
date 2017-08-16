package org.alliancegenome.indexer.entity;

import org.neo4j.ogm.annotation.NodeEntity;

import lombok.Getter;
import lombok.Setter;

@NodeEntity
@Getter @Setter
public class DOTerm extends Neo4jNode {

	private String doUrl;
	private String doDisplayId;
	private String doId;
	private String doPrefix;
	private String primaryKey;
	
}
