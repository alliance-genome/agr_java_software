package org.alliancegenome.neo4j.entity.node;

import java.util.*;

import org.alliancegenome.neo4j.entity.*;
import org.alliancegenome.neo4j.view.View;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.neo4j.ogm.annotation.*;

import com.fasterxml.jackson.annotation.*;

import lombok.*;

@NodeEntity
@Getter
@Setter
@Schema(name="Species", description="POJO that represents the Species")
public class Species extends Neo4jEntity implements Comparable<Species> {

    @JsonView({View.Default.class})
    @JsonProperty(value = "taxonId")
    private String primaryKey;

    @JsonView({View.Default.class})
    private String name;
    @JsonView({View.Default.class})
    private String shortName;
    @JsonView({View.Default.class})
    private String dataProviderFullName;
    @JsonView({View.Default.class})
    private String dataProviderShortName;
    
    @JsonView({View.DiseaseCacher.class, View.OrthologyCacher.class})
    private int phylogeneticOrder;

    @JsonView({View.Default.class})
    private String commonNames;

    @Relationship(type = "CREATED_BY")
    private Set<Gene> genes = new HashSet<>();

    public static Species getSpeciesFromTaxonId(String taxonID) {
        for (SpeciesType species : SpeciesType.values()) {
            if (species.getTaxonID().equals(taxonID)) {
                Species spec = new Species();
                spec.setPrimaryKey(taxonID); // Needed for the download file
                spec.setName(species.getName());
                return spec;
            }
        }
        return null;
    }

    @Override
    public int compareTo(Species species1) {
        return getType().compareTo(species1.getType());
    }

    public SpeciesType getType() {
        return SpeciesType.getTypeByName(this.name);
    }

    @Override
    public String toString() {
        return primaryKey + " : " + name;
    }
}
