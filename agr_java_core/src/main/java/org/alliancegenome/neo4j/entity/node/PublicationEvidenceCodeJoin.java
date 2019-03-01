package org.alliancegenome.neo4j.entity.node;

import lombok.Getter;
import lombok.Setter;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import java.util.List;

@NodeEntity
@Getter
@Setter
public class PublicationEvidenceCodeJoin extends Association {

    protected String primaryKey;
    protected String joinType;

    @Relationship(type = "ASSOCIATION", direction = Relationship.INCOMING)
    private Publication publication;

    @Relationship(type = "ASSOCIATION")
    private List<EvidenceCode> evidenceCodes;


}
