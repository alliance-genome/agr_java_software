package org.alliancegenome.neo4j.entity.node;

import org.alliancegenome.neo4j.entity.Neo4jEntity;
import org.alliancegenome.neo4j.view.View;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.neo4j.ogm.annotation.NodeEntity;

import com.fasterxml.jackson.annotation.JsonView;

import lombok.*;

@Getter @Setter
@NodeEntity
@Schema(name="Chromosome", description="POJO that represents the Chromosome")
public class Chromosome extends Neo4jEntity {
    
    @JsonView({View.Default.class})
    private String primaryKey;
}
