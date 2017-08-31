package org.alliancegenome.indexer.entity.node;

import org.alliancegenome.indexer.entity.Neo4jEntity;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class Entity extends Neo4jEntity {
	
	private String release;
	//private Date dateProduced;
	private String primaryKey;
	
//	@Relationship(type = "CREATED_BY", direction=Relationship.INCOMING)
//	private Set<Gene> createdBy = new HashSet<Gene>();

}
