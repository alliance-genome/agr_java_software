package org.alliancegenome.neo4j.entity.node;

import lombok.Getter;
import lombok.Setter;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import java.util.List;

@NodeEntity
@Getter
@Setter
public class BioEntityGeneExpressionJoin extends Association {

    @Relationship(type = "ASSOCIATION", direction = Relationship.INCOMING)
    private ExpressionBioEntity entity;

    @Relationship(type = "EVIDENCE")
    private Publication publication;

    @Relationship(type = "DURING")
    private Stage stage;

    @Relationship(type = "STAGE_RIBBON_TERM")
    private UBERONTerm stageTerm;

    @Relationship(type = "ASSAY")
    private MMOTerm assay;

    @Relationship(type = "ASSOCIATION", direction = Relationship.INCOMING)
    private Gene gene;


}
