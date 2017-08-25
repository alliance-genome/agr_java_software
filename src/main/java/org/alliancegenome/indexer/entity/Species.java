package org.alliancegenome.indexer.entity;

import lombok.Getter;
import lombok.Setter;
import org.neo4j.ogm.annotation.Relationship;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
public class Species extends Neo4jNode implements Comparable<Species> {

    private String species;
    private String primaryId;
    private String name;

    @Relationship(type = "CREATED_BY")
    private Set<Gene> genes = new HashSet<>();


    @Override
    public int compareTo(Species species1) {
        return getType().compareTo(species1.getType());
    }

    Type getType() {
        return Type.getTypeByName(this.species);
    }

    enum Type {
        HUMAN("Homo sapiens", "NCBITaxon:9606"),
        RAT("Rattus norvegius", "NCBITaxon:10116"),
        MOUSE("Mus musculus", "NCBITaxon:10090"),
        ZEBRAFISH("Danio rerio", "NCBITaxon:7955"),
        FLY("Drosophila melanogaster", "NCBITaxon:7227"),
        WORM("Chaenorhabditis elegans", "NCBITaxon:6239"),
        YEAST("Saccharomyces cerevisiae", "NCBITaxon:4932"),;

        String name;
        String taxinID;

        Type(String name, String taxinID) {
            this.name = name;
            this.taxinID = taxinID;
        }

        public static Type getTypeByName(String name) {
            for (Type type : values())
                if (type.name.equals(name))
                    return type;
            return null;
        }
    }
}
