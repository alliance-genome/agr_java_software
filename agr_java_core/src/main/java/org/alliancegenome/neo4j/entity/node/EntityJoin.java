package org.alliancegenome.neo4j.entity.node;

import lombok.Getter;
import lombok.Setter;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import java.util.List;

@NodeEntity
@Getter
@Setter
public class EntityJoin extends Association {

    protected String primaryKey;
    protected String joinType;

    @Relationship(type = "EVIDENCE")
    private Publication publication;

    @Relationship(type = "EVIDENCE")
    private List<EvidenceCode> evidenceCodes;

    @Relationship(type = "ASSOCIATION", direction = Relationship.INCOMING)
    private Gene gene;

    @Relationship(type = "ASSOCIATION", direction = Relationship.INCOMING)
    private Feature feature;

}
