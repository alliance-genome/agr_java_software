package org.alliancegenome.indexer.entity.node;

import lombok.Getter;
import lombok.Setter;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import java.util.List;

@NodeEntity
@Getter
@Setter
public class DiseaseGeneJoin extends Association {

    private String joinType;

    @Relationship(type = "ASSOCIATION")
    private Publication publication;

    @Relationship(type = "ASSOCIATION")
    private List<EvidenceCode> evidenceCodes;

    @Relationship(type = "ASSOCIATION", direction = Relationship.INCOMING)
    private Gene gene;

}
