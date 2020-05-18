package org.alliancegenome.neo4j.entity.node;

import java.util.ArrayList;
import java.util.List;

import org.alliancegenome.neo4j.view.View;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;

import lombok.Getter;
import lombok.Setter;

@NodeEntity(label = "AffectedGenomicModel")
@Getter
@Setter
@Schema(name="AffectedGenomicModel", description="POJO that represents the Affected Genomic model")
public class AffectedGenomicModel extends GeneticEntity implements Comparable<AffectedGenomicModel> {

    public AffectedGenomicModel() {
        this.crossReferenceType = CrossReferenceType.ALLELE;
    }

    private String release;
    private String localId;
    private String globalId;
    @JsonView({View.Default.class, View.API.class})
    private String modCrossRefCompleteUrl;
    @JsonView({View.Default.class, View.API.class})
    private String name;
    @JsonView({View.Default.class, View.API.class})
    private String nameText;
    private String nameTextWithSpecies;
    @JsonProperty(value = "type")
    @JsonView({View.Default.class, View.API.class})
    private String subtype;
    private String dataProvider;

    @Relationship(type = "PRIMARY_GENETIC_ENTITY", direction = Relationship.INCOMING)
    private List<DiseaseEntityJoin> diseaseEntityJoins = new ArrayList<>();

    @Relationship(type = "SEQUENCE_TARGETING_REAGENT")
    private List<SequenceTargetingReagent> sequenceTargetingReagents = new ArrayList<>();

    @Relationship(type = "MODEL_COMPONENT")
    private List<Allele> alleles = new ArrayList<>();

    @Override
    public int compareTo(AffectedGenomicModel o) {
        return 0;
    }

}
