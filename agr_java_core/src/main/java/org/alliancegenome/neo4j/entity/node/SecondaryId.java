package org.alliancegenome.neo4j.entity.node;

import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.neo4j.ogm.annotation.NodeEntity;

import lombok.Getter;
import lombok.Setter;

@NodeEntity
@Getter @Setter
@Schema(name="SecondaryId", description="POJO that represents the Secondary Id")
public class SecondaryId extends Identifier {

    private String primaryKey;
    private String name;
}
