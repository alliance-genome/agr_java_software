package org.alliancegenome.neo4j.entity.node;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.Getter;
import lombok.Setter;
import org.alliancegenome.neo4j.entity.Neo4jEntity;
import org.alliancegenome.neo4j.view.View;
import org.neo4j.ogm.annotation.NodeEntity;

@NodeEntity(label = "GenomicLocation")
@Getter
@Setter
public class GenomicLocation extends Neo4jEntity implements Comparable<GenomicLocation> {

    @JsonView({View.Default.class, View.API.class})
    @JsonProperty(value = "id")
    protected String primaryKey;

    @JsonView({View.Default.class, View.API.class})
    private Long start;
    @JsonView({View.Default.class, View.API.class})
    private Long end;
    @JsonView({View.Default.class, View.API.class})
    private String chromosome;
    private String assembly;

    @Override
    public int compareTo(GenomicLocation o) {
        return 0;
    }

}
