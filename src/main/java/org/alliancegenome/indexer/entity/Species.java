package org.alliancegenome.indexer.entity;

import java.util.HashSet;
import java.util.Set;

import org.neo4j.ogm.annotation.Relationship;

import lombok.Data;

@Data
public class Species extends Neo4jNode {

	private String species;
	private String primaryId;
	private String name;
	
	@Relationship(type = "CREATED_BY")
	private Set<Gene> genes = new HashSet<>();
}
