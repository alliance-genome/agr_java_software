package org.alliancegenome.neo4j.entity.node;

import com.fasterxml.jackson.annotation.JsonView;
import lombok.Getter;
import lombok.Setter;
import org.alliancegenome.neo4j.view.View;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

@NodeEntity(label = "SequenceTargetingReagent")
@Getter
@Setter
public class SequenceTargetingReagent extends GeneticEntity implements Comparable<SequenceTargetingReagent> {

    public SequenceTargetingReagent() {
        this.crossReferenceType = CrossReferenceType.FISH;
    }

    private String release;
    private String localId;
    private String globalId;
    private String modCrossRefCompleteUrl;
    @JsonView({View.Default.class})
    private String name;

    @Relationship(type = "TARGETS")
    private Gene gene;

    @Override
    public int compareTo(SequenceTargetingReagent o) {
        return 0;
    }

}
