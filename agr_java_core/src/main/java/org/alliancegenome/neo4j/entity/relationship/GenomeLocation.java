package org.alliancegenome.neo4j.entity.relationship;

import org.alliancegenome.neo4j.entity.Neo4jEntity;
import org.alliancegenome.neo4j.entity.node.Chromosome;
import org.alliancegenome.neo4j.view.View;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;

import lombok.Getter;
import lombok.Setter;

@NodeEntity(label = "GenomicLocation")
@Getter
@Setter
public class GenomeLocation extends Neo4jEntity {

    @Relationship(type = "ASSOCIATION")
    private Chromosome chromosome;

    @JsonView({View.Default.class})
    @JsonProperty(value = "chromosome")
    public String getChromosomeName() {
        return chromosome.getPrimaryKey();
    }

    @JsonProperty(value = "chromosome")
    public void setChromosomeName(String name) {
        chromosome = new Chromosome();
        chromosome.setPrimaryKey(name);
    }

    @JsonView({View.Default.class})
    private Long start;

    @JsonView({View.Default.class})
    private Long end;

    @JsonView({View.Default.class})
    private String assembly;

    private String strand;
}
