package org.alliancegenome.indexer.entity;

import lombok.Getter;
import lombok.Setter;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import java.util.List;

@NodeEntity
@Getter
@Setter
public class Annotation extends Neo4jNode {

    @Relationship(type = "ASSOCIATION", direction = Relationship.OUTGOING)
    private List<Publication> publications;

    @Relationship(type = "ASSOCIATION", direction = Relationship.INCOMING)
    private Gene gene;

}
