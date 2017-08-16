package org.alliancegenome.indexer.entity;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class Entity extends Neo4jNode {
	
	private String release;
	//private Date dateProduced;
	private String primaryKey;
	
//	@Relationship(type = "CREATED_BY", direction=Relationship.INCOMING)
//	private Set<Gene> createdBy = new HashSet<Gene>();

}
