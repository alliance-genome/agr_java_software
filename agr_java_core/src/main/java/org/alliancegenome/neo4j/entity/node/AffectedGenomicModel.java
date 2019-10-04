package org.alliancegenome.neo4j.entity.node;

import lombok.Getter;
import lombok.Setter;
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
    private String modCrossRefCompleteUrl;
    private String name;
    private String nameText;
    private String nameTextWithSpecies;

    @Relationship(type = "PRIMARY_GENETIC_ENTITY", direction = Relationship.INCOMING)
    private List<DiseaseEntityJoin> diseaseEntityJoins = new ArrayList<>();

    @Override
    public int compareTo(AffectedGenomicModel o) {
        return 0;
    }

}
