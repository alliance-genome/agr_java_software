package org.alliancegenome.indexer.entity.node;

import org.alliancegenome.indexer.entity.Neo4jEntity;
import org.neo4j.ogm.annotation.NodeEntity;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@NodeEntity
public class CrossReference extends Neo4jEntity {
    
    private String crossRefCompleteUrl;
    private String localId;
    private String globalCrossRefId;
    private String prefix;
    private String name;
    private String primaryKey;
    
}
