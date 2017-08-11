package org.alliancegenome.indexer.entity;

import lombok.Data;

@Data
public class Entity extends Neo4jNode {
	
	private String release;
	//private Date dateProduced;
	private String primaryKey;
	
//	@Relationship(type = "CREATED_BY", direction=Relationship.INCOMING)
//	private Set<Gene> createdBy = new HashSet<Gene>();

}
