package org.alliancegenome.neo4j.entity.node;

import lombok.Getter;
import lombok.Setter;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

@NodeEntity
@Getter
@Setter
public class BioEntityGeneExpressionJoin extends EntityJoin {

    @Relationship(type = "ASSOCIATION")
    private ExpressionBioEntity entity;

}
