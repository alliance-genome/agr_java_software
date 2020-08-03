package org.alliancegenome.neo4j.entity.node;

import com.fasterxml.jackson.annotation.JsonView;
import lombok.Getter;
import lombok.Setter;
import org.alliancegenome.api.entity.PresentationEntity;
import org.alliancegenome.neo4j.view.View;
import org.apache.commons.collections.CollectionUtils;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import java.util.ArrayList;
import java.util.List;

@NodeEntity(label = "Feature")
@Getter
@Setter
@Schema(name = "Allele", description = "POJO that represents the Allele")
public class Allele extends GeneticEntity implements Comparable<Allele>, PresentationEntity {

    public Allele() {
        this.crossReferenceType = CrossReferenceType.ALLELE;
    }

    private String release;
    private String localId;
    private String globalId;
    private String modCrossRefCompleteUrl;
    @JsonView({View.Default.class})
    private String symbolText;
    private String symbolTextWithSpecies;
    @JsonView({View.AlleleAPI.class})
    private String description;

    @JsonView({View.AlleleAPI.class, View.TransgenicAlleleAPI.class})
    @Relationship(type = "IS_ALLELE_OF")
    private Gene gene;

    @JsonView({View.GeneAllelesAPI.class})
    @Relationship(type = "IS_IMPLICATED_IN", direction = Relationship.INCOMING)
    private List<DOTerm> diseases = new ArrayList<>();

    @JsonView({View.GeneAllelesAPI.class})
    @Relationship(type = "VARIATION", direction = Relationship.INCOMING)
    private List<Variant> variants = new ArrayList<>();

    @Relationship(type = "ASSOCIATION", direction = Relationship.UNDIRECTED)
    private List<DiseaseEntityJoin> diseaseEntityJoins = new ArrayList<>();

    @JsonView({View.GeneAllelesAPI.class})
    @Relationship(type = "HAS_PHENOTYPE")
    private List<Phenotype> phenotypes = new ArrayList<>();

    @JsonView({View.AlleleAPI.class, View.TransgenicAlleleAPI.class})
    @Relationship(type = "CONTAINS")
    private List<Construct> constructs;

    @Override
    public int compareTo(Allele o) {
        return 0;
    }

    @Override
    public String toString() {
        return primaryKey + ":" + symbolText;
    }

    @JsonView({View.TransgenicAlleleAPI.class})
    public Boolean hasPhenotype() {
        return CollectionUtils.isNotEmpty(phenotypes);
    }

    @JsonView({View.TransgenicAlleleAPI.class})
    public Boolean hasDisease() {
        return CollectionUtils.isNotEmpty(diseaseEntityJoins);
    }
}
