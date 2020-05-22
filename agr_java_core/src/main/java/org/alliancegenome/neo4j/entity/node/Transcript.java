package org.alliancegenome.neo4j.entity.node;

import java.util.List;

import org.alliancegenome.neo4j.entity.Neo4jEntity;
import org.alliancegenome.neo4j.view.View;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;

import lombok.Getter;
import lombok.Setter;

@NodeEntity(label = "Transcript")
@Getter
@Setter
public class Transcript extends Neo4jEntity implements Comparable<Transcript> {

    @JsonView({View.Default.class, View.API.class})
    @JsonProperty(value = "id")
    protected String primaryKey;

    @JsonView({View.Default.class, View.API.class})
    protected String name;

    @Override
    public int compareTo(Transcript o) {
        return name.compareTo(o.getName());
    }

    @JsonView({View.Default.class, View.API.class})
    @Relationship(type = "ASSOCIATION")
    private List<TranscriptLevelConsequence> consequences;

    @JsonView({View.Default.class, View.API.class})
    @Relationship(type = "TRANSCRIPT_TYPE", direction = Relationship.INCOMING)
    private SOTerm type;

    @JsonView({View.Default.class, View.API.class})
    @Relationship(type = "TRANSCRIPT")
    private Gene gene;

}
