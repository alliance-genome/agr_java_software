package org.alliancegenome.neo4j.entity.node;

import org.alliancegenome.neo4j.entity.Neo4jEntity;
import org.alliancegenome.neo4j.view.View;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.neo4j.ogm.annotation.NodeEntity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;

import lombok.Getter;
import lombok.Setter;

@NodeEntity(label = "Note")
@Getter
@Setter
@Schema(name = "Note", description = "POJO that represents a Note")
public class Note extends Neo4jEntity {

    @JsonView({View.Default.class, View.API.class})
    @JsonProperty(value = "id")
    protected String primaryKey;

    @JsonView({View.Default.class, View.API.class})
    protected String note;

}
