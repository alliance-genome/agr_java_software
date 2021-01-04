package org.alliancegenome.neo4j.entity.node;

import java.util.List;

import org.alliancegenome.api.entity.PresentationEntity;
import org.alliancegenome.neo4j.entity.Neo4jEntity;
import org.alliancegenome.neo4j.view.View;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.neo4j.ogm.annotation.*;

import com.fasterxml.jackson.annotation.JsonView;

import lombok.*;

@Getter
@Setter
@NodeEntity
@Schema(name="InteractionGeneJoin", description="POJO that represents the Interaction-Gene join")
public class InteractionGeneJoin extends Neo4jEntity implements Comparable, PresentationEntity {

    @JsonView({View.Interaction.class})
    private String primaryKey;
    
    @JsonView({View.Interaction.class})
    private String joinType;

    @JsonView({View.Interaction.class})
    @Relationship(type = "ASSOCIATION", direction = Relationship.INCOMING)
    private Gene geneA;

    @JsonView({View.Interaction.class})
    @Relationship(type = "ASSOCIATION")
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
    private MITerm interactorAType;

    @JsonView({View.Interaction.class})
    @Relationship(type = "INTERACTOR_A_ROLE")
    private MITerm interactorARole;

    @JsonView({View.Interaction.class})
    @Relationship(type = "INTERACTOR_B_TYPE")
    private MITerm interactorBType;

    @JsonView({View.Interaction.class})
    @Relationship(type = "INTERACTOR_B_ROLE")
    private MITerm interactorBRole;
  
    @JsonView({View.Interaction.class})
    @Relationship(type = "INTERACTOR_A_GENETIC_PERTURBATION", direction = Relationship.INCOMING)
    private Allele alleleA;

    @JsonView({View.Interaction.class})
    @Relationship(type = "INTERACTOR_B_GENETIC_PERTURBATION")
    private Allele alleleB;
    
 
    
    @JsonView({View.Interaction.class})
    @Relationship(type = "PHENOTYPE_TRAIT")
    private Phenotype phenotype;

    @Override
    public String toString() {
        return geneA.getSymbol() + " : " + geneB.getSymbol();
    }

    @Override
    public int compareTo(Object o) {
        return 0;
    }
}