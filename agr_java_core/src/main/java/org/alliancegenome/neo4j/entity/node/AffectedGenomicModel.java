package org.alliancegenome.neo4j.entity.node;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.Getter;
import lombok.Setter;
import org.alliancegenome.neo4j.view.View;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import java.util.ArrayList;
import java.util.List;

@NodeEntity(label = "AffectedGenomicModel")
@Getter
@Setter
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
    @JsonProperty(value = "type")
    @JsonView({View.Default.class, View.API.class})
    private String subtype;

    @Relationship(type = "PRIMARY_GENETIC_ENTITY", direction = Relationship.INCOMING)
    private List<DiseaseEntityJoin> diseaseEntityJoins = new ArrayList<>();

    @Override
    public int compareTo(AffectedGenomicModel o) {
        return 0;
    }

}
