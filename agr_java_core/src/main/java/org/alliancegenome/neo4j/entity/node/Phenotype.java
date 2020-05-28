package org.alliancegenome.neo4j.entity.node;

import java.util.List;

import org.alliancegenome.neo4j.entity.Neo4jEntity;
import org.alliancegenome.neo4j.view.View;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import com.fasterxml.jackson.annotation.JsonView;

import lombok.Getter;
import lombok.Setter;

@NodeEntity
@Getter
@Setter
@Schema(name="Phenotype", description="POJO that represents the Phenotype")
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
