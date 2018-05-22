package org.alliancegenome.neo4j.entity.node;

import java.util.HashSet;
import java.util.Set;

import org.alliancegenome.neo4j.entity.Neo4jEntity;
import org.alliancegenome.neo4j.entity.SpeciesType;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import lombok.Getter;
import lombok.Setter;

@NodeEntity
@Getter
@Setter
public class Species extends Neo4jEntity implements Comparable<Species> {

    private String species;
    private String primaryKey;
    private String name;

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
