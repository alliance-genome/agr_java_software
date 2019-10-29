package org.alliancegenome.neo4j.entity.node;

import com.fasterxml.jackson.annotation.JsonView;
import lombok.Getter;
import lombok.Setter;
import org.alliancegenome.neo4j.entity.Neo4jEntity;
import org.alliancegenome.neo4j.view.View;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import java.util.List;

@NodeEntity
@Getter
@Setter
public class Phenotype extends Neo4jEntity {

    private String primaryKey;
    @JsonView({View.GeneAllelesAPI.class, View.AlleleAPI.class})
    private String phenotypeStatement;

    @Relationship(type = "IS_IMPLICATED_IN", direction = Relationship.INCOMING)
    private List<Gene> genes;

    @Relationship(type = "ASSOCIATION", direction = Relationship.INCOMING)
    private List<PhenotypeEntityJoin> phenotypeEntityJoins;

    @Override
    public String toString() {
        return phenotypeStatement;
    }
}
