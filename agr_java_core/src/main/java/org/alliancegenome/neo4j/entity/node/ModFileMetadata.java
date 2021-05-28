package org.alliancegenome.neo4j.entity.node;

import java.util.Date;

import org.alliancegenome.es.util.DateConverter;
import org.alliancegenome.neo4j.entity.Neo4jEntity;
import org.alliancegenome.neo4j.view.View;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.typeconversion.Convert;

import com.fasterxml.jackson.annotation.JsonView;

import lombok.*;


@NodeEntity
@Getter
@Setter
@Schema(name="ModFileMetadata", description="POJO that represents the ModFileMetaData")
public class ModFileMetadata extends Neo4jEntity {

    @JsonView({View.API.class})
    @Convert(value = DateConverter.class)
    private Date date_produced;
    @JsonView({View.API.class})
    private String mod;
    @JsonView({View.API.class})
    private String release;
    @JsonView({View.API.class})
    private String type;

}