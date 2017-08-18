package org.alliancegenome.indexer.entity;

import lombok.Getter;
import lombok.Setter;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import java.util.List;

@NodeEntity
@Getter
@Setter
public class Publication extends Neo4jNode {

    private String primaryKey;
    private String pubMedId;
    @Relationship(type = "ANNOTATED_TO")
    private List<EvidenceCode> evidence;

}
