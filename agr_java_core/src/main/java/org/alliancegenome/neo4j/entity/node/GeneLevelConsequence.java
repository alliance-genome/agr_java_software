package org.alliancegenome.neo4j.entity.node;

import org.alliancegenome.neo4j.entity.Neo4jEntity;
import org.alliancegenome.neo4j.view.View;
import org.neo4j.ogm.annotation.NodeEntity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;

import lombok.Getter;
import lombok.Setter;

@NodeEntity(label = "GeneLevelConsequence")
@Getter
@Setter
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
