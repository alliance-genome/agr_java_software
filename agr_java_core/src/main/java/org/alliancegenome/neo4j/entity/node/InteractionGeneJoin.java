package org.alliancegenome.neo4j.entity.node;

import java.util.List;

import org.alliancegenome.neo4j.view.View;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import com.fasterxml.jackson.annotation.JsonView;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@NodeEntity
public class InteractionGeneJoin extends Association {
    
    @JsonView({View.Interaction.class})
    private String primaryKey;
    @JsonView({View.Interaction.class})
    private String joinType;

    @JsonView({View.Interaction.class})
    @Relationship(type = "ASSOCIATION", direction = Relationship.INCOMING)
    private Gene geneA;

    @JsonView({View.Interaction.class})
    @Relationship(type = "ASSOCIATION", direction = Relationship.OUTGOING)
    private Gene geneB;

    @JsonView({View.Interaction.class})
    @Relationship(type = "CROSS_REFERENCE")
    private List<CrossReference> crossReferences;

    @JsonView({View.Interaction.class})
    @Relationship(type = "EVIDENCE")
    private Publication publication;

    @JsonView({View.Interaction.class})
    @Relationship(type = "SOURCE_DATABASE")
    private MITerm sourceDatabase;

    @JsonView({View.Interaction.class})
    @Relationship(type = "AGGREGATION_DATABASE")
    private MITerm aggregationDatabase;

    @JsonView({View.Interaction.class})
    @Relationship(type = "DETECTION_METHOD")
    private List<MITerm> detectionsMethods;

    @JsonView({View.Interaction.class})
    @Relationship(type = "INTERACTION_TYPE")
    private MITerm interactionType;

    @JsonView({View.Interaction.class})
    @Relationship(type = "INTERACTOR_A_TYPE")
    private MITerm interactionAType;

    @JsonView({View.Interaction.class})
    @Relationship(type = "INTERACTOR_A_ROLE")
    private MITerm interactionARole;

    @JsonView({View.Interaction.class})
    @Relationship(type = "INTERACTOR_B_TYPE")
    private MITerm interactionBType;

    @JsonView({View.Interaction.class})
    @Relationship(type = "INTERACTOR_B_ROLE")
    private MITerm interactionBRole;
    
}