package org.alliancegenome.indexer.entity.node;

import lombok.Getter;
import lombok.Setter;
import org.alliancegenome.indexer.entity.Neo4jEntity;
import org.alliancegenome.indexer.entity.SpeciesType;
import org.neo4j.ogm.annotation.Relationship;

import java.util.HashSet;
import java.util.Set;

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
