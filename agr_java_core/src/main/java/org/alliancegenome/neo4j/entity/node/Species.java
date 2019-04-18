package org.alliancegenome.neo4j.entity.node;

import java.util.HashSet;
import java.util.Set;

import org.alliancegenome.neo4j.entity.Neo4jEntity;
import org.alliancegenome.neo4j.entity.SpeciesType;
import org.alliancegenome.neo4j.view.View;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;

import lombok.Getter;
import lombok.Setter;

@NodeEntity
@Getter
@Setter
public class Species extends Neo4jEntity implements Comparable<Species> {

    @JsonView({View.Default.class})
    @JsonProperty(value = "taxonId")
    private String primaryKey;

    @JsonView({View.Default.class})
    private String name;
    private int phylogeneticOrder;

    private String species;

    @Relationship(type = "CREATED_BY")
    private Set<Gene> genes = new HashSet<>();

    @Override
    public int compareTo(Species species1) {
        return getType().compareTo(species1.getType());
    }

    public SpeciesType getType() {
        return SpeciesType.getTypeByName(this.species);
    }

}
