package org.alliancegenome.neo4j.entity.node;

import java.util.List;

import org.alliancegenome.neo4j.entity.Neo4jEntity;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import lombok.Getter;
import lombok.Setter;

@NodeEntity
@Getter
@Setter
public class Phenotype extends Neo4jEntity {

    private String primaryKey;
    private String phenotypeStatement;

    @Relationship(type = "IS_IMPLICATED_IN", direction = Relationship.INCOMING)
    private List<Gene> genes;

    @Relationship(type = "ASSOCIATION", direction = Relationship.INCOMING)
    private List<PhenotypeEntityJoin> phenotypeEntityJoins;

}
