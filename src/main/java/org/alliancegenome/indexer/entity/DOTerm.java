package org.alliancegenome.indexer.entity;

import lombok.Getter;
import lombok.Setter;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import java.util.List;

@NodeEntity
@Getter
@Setter
public class DOTerm extends Neo4jNode {

    private String doUrl;
    private String doDisplayId;
    private String doId;
    private String doPrefix;
    private String primaryKey;
    private String name;

    @Relationship(type = "IS_IMPLICATED_IN", direction = Relationship.INCOMING)
    private List<Gene> genes;

    @Relationship(type = "ASSOCIATION", direction = Relationship.INCOMING)
    private List<Annotation> annotations;

    @Relationship(type = "IS_A", direction = Relationship.INCOMING)
    private List<DOTerm> children;

    @Relationship(type = "IS_A")
    private List<DOTerm> parents;

}
