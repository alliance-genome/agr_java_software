package org.alliancegenome.neo4j.entity.node;

import java.util.List;

import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import lombok.Getter;
import lombok.Setter;

@NodeEntity
@Getter
@Setter
public class PublicationEvidenceCodeJoin extends Association {

    protected String primaryKey;
    protected String joinType;

    @Relationship(type = "ASSOCIATION", direction = Relationship.INCOMING)
    private Publication publication;

    @Relationship(type = "ASSOCIATION")
    private List<ECOTerm> ecoCode;

}
