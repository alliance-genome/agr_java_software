package org.alliancegenome.neo4j.entity.node;

import org.alliancegenome.neo4j.entity.Neo4jEntity;
import org.alliancegenome.neo4j.view.View;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.neo4j.ogm.annotation.NodeEntity;

import com.fasterxml.jackson.annotation.*;

import lombok.*;

@NodeEntity(label = "GeneLevelConsequence")
@Getter
@Setter
@Schema(name="GeneLevelConsequence", description="POJO that represents Gene Level Consequences")
public class GeneLevelConsequence extends Neo4jEntity implements Comparable<GeneLevelConsequence> {

    @JsonView({View.Default.class, View.API.class})
    @JsonProperty(value = "id")
    protected String primaryKey;

    @JsonView({View.Default.class, View.API.class})
    @JsonProperty(value = "consequence")
    private String geneLevelConsequence;

    @Override
    public int compareTo(GeneLevelConsequence o) {
        return 0;
    }

}
