package org.alliancegenome.indexer.entity.node;

import org.neo4j.ogm.annotation.NodeEntity;

import lombok.Getter;
import lombok.Setter;

@NodeEntity
@Getter @Setter
public class Synonym extends Identifier {

    private String primaryKey;
    private String name;
}
