package org.alliancegenome.indexer.entity.node;

import lombok.Getter;
import lombok.Setter;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import java.util.List;

@NodeEntity
@Getter
@Setter
public class DiseaseFeatureJoin extends Association {

    private String primaryKey;
    private String joinType;

    @Relationship(type = "EVIDENCE")
    private Publication publication;

    @Relationship(type = "EVIDENCE")
    private List<EvidenceCode> evidenceCodes;

    @Relationship(type = "ASSOCIATION", direction = Relationship.INCOMING)
    private Feature feature;

}
