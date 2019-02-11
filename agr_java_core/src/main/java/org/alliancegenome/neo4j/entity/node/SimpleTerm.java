package org.alliancegenome.neo4j.entity.node;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.Getter;
import lombok.Setter;
import org.alliancegenome.neo4j.entity.Neo4jEntity;
import org.alliancegenome.neo4j.view.View;
import org.neo4j.ogm.annotation.NodeEntity;

@NodeEntity
@Getter
@Setter
public class SimpleTerm extends Neo4jEntity {

    @JsonView({View.API.class, View.DiseaseAnnotation.class})
    @JsonProperty(value = "id")
    protected String primaryKey;

    @JsonView({View.API.class, View.DiseaseAnnotation.class})
    protected String name;

}
