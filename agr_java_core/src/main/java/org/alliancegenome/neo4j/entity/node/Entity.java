package org.alliancegenome.neo4j.entity.node;

import org.alliancegenome.neo4j.entity.Neo4jEntity;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.neo4j.ogm.annotation.NodeEntity;

import lombok.*;

@NodeEntity
@Getter @Setter
@Schema(name="Entity", description="POJO that represents the Entity")
public class Entity extends Neo4jEntity {
    
    private String release;
    //private Date dateProduced;
    private String primaryKey;
    
//  @Relationship(type = "CREATED_BY", direction=Relationship.INCOMING)
//  private Set<Gene> createdBy = new HashSet<Gene>();

}
