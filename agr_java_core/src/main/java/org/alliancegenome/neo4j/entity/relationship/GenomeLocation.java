package org.alliancegenome.neo4j.entity.relationship;

import org.alliancegenome.neo4j.entity.Neo4jEntity;
import org.alliancegenome.neo4j.entity.node.Chromosome;
import org.alliancegenome.neo4j.entity.node.Gene;
import org.neo4j.ogm.annotation.EndNode;
import org.neo4j.ogm.annotation.RelationshipEntity;
import org.neo4j.ogm.annotation.StartNode;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@RelationshipEntity(type="LOCATED_ON")
public class GenomeLocation extends Neo4jEntity {

    @StartNode
    private Gene gene;
    @EndNode
    private Chromosome chromosome;
    
    private Long start;
    private Long end;
    private String strand;
    private String assembly;
}
