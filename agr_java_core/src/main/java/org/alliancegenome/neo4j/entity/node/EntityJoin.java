package org.alliancegenome.neo4j.entity.node;

import lombok.Getter;
import lombok.Setter;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import java.util.List;
import java.util.Set;

@NodeEntity
@Getter
@Setter
public class EntityJoin extends Association {

    protected String primaryKey;
    protected String joinType;

    @Relationship(type = "EVIDENCE", direction = Relationship.INCOMING)
    private List<Publication> publications;

    @Relationship(type = "EVIDENCE")
    private List<ECOTerm> evidenceCodes;

    @Relationship(type = "ASSOCIATION", direction = Relationship.INCOMING)
    private Gene gene;

    @Relationship(type = "ASSOCIATION", direction = Relationship.INCOMING)
    private Allele allele;

    // direct annotations
    @Relationship(type = "ASSOCIATION", direction = Relationship.INCOMING)
    private AffectedGenomicModel model;

}
