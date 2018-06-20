package org.alliancegenome.neo4j.entity.node;

import lombok.Getter;
import lombok.Setter;
import org.alliancegenome.es.util.DateConverter;
import org.alliancegenome.neo4j.entity.Neo4jEntity;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;
import org.neo4j.ogm.annotation.typeconversion.Convert;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
