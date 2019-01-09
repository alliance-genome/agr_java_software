package org.alliancegenome.neo4j.entity.relationship;

import org.alliancegenome.neo4j.entity.Neo4jEntity;
import org.alliancegenome.neo4j.entity.node.Chromosome;
import org.alliancegenome.neo4j.entity.node.Gene;
import org.alliancegenome.neo4j.view.View;
import org.neo4j.ogm.annotation.EndNode;
import org.neo4j.ogm.annotation.RelationshipEntity;
import org.neo4j.ogm.annotation.StartNode;

import com.fasterxml.jackson.annotation.JsonView;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@RelationshipEntity(type="LOCATED_ON")
public class GenomeLocation extends Neo4jEntity {

    @StartNode
    private Gene gene;

    @EndNode
    @JsonView(value={View.Default.class})
    private Chromosome chromosome;

    @JsonView(value={View.Default.class})
    private Long start;

    @JsonView(value={View.Default.class})
    private Long end;

    @JsonView(value={View.Default.class})
    private String assembly;

    private String strand;
}
