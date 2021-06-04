package org.alliancegenome.neo4j.entity.node;

import java.util.Date;

import org.alliancegenome.es.util.DateConverter;
import org.alliancegenome.neo4j.entity.Neo4jEntity;
import org.alliancegenome.neo4j.view.View;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.typeconversion.Convert;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;

import lombok.*;


@NodeEntity
@Getter
@Setter
@Schema(name="ModFileMetadata", description="POJO that represents the ModFileMetaData")
public class ModFileMetadata extends Neo4jEntity {

    @JsonView({View.API.class})
    @Convert(value = DateConverter.class)
    @JsonProperty(value = "releaseDate")
    private Date date_produced;
    @JsonView({View.API.class})
    @JsonProperty(value = "mod")
    private String dataSubType;
    @JsonView({View.API.class})
    @JsonProperty(value = "releaseVersion")
    private String release;
    @JsonView({View.API.class})
    @JsonProperty(value = "type")
    private String data;

}