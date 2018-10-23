package org.alliancegenome.neo4j.entity.node;

import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import lombok.Getter;
import lombok.Setter;

@NodeEntity
@Getter
@Setter
public class DiseaseEntityJoin extends EntityJoin {

    @Relationship(type = "ASSOCIATION")
    private DOTerm disease;

    @Relationship(type = "FROM_ORTHOLOGOUS_GENE")
    private Gene orthologyGene;

    private String dataProvider;
}
