package org.alliancegenome.neo4j.entity.node;

import org.alliancegenome.neo4j.entity.Neo4jEntity;
import org.alliancegenome.neo4j.view.View;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.neo4j.ogm.annotation.NodeEntity;

import com.fasterxml.jackson.annotation.*;

import lombok.*;

@NodeEntity
@Getter
@Setter
@Schema(name="SimpleTerm", description="POJO that represents a Simple Term")
public class SimpleTerm extends Neo4jEntity {

    @JsonView({View.DiseaseAPI.class, View.DiseaseAnnotation.class, View.API.class})
    @JsonProperty(value = "id")
    protected String primaryKey;

    @JsonView({View.DiseaseAPI.class, View.DiseaseAnnotation.class, View.API.class})
    protected String name;

}
