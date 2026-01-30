package org.alliancegenome.neo4j.entity.node;

import java.util.ArrayList;
import java.util.List;

import org.alliancegenome.api.entity.PresentationEntity;
import org.alliancegenome.neo4j.entity.Neo4jEntity;
import org.alliancegenome.neo4j.view.PublicView;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import com.fasterxml.jackson.annotation.JsonView;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@NodeEntity
@Schema(name = "InteractionGeneJoin", description = "POJO that represents the Interaction-Gene join")
public class InteractionGeneJoin extends Neo4jEntity implements Comparable, PresentationEntity {

	@JsonView({PublicView.Interaction.class})
	private String primaryKey;
	
	@JsonView({PublicView.Interaction.class})
	private String joinType;

	@JsonView({PublicView.Interaction.class})
	@Relationship(type = "ASSOCIATION", direction = Relationship.Direction.INCOMING)
	private Gene geneA;

	@JsonView({PublicView.Interaction.class})
	@Relationship(type = "ASSOCIATION")
	private Gene geneB;

	@JsonView({PublicView.Interaction.class})
	@Relationship(type = "CROSS_REFERENCE")
	private List<CrossReference> crossReferences;

	@JsonView({PublicView.Interaction.class})
	@Relationship(type = "EVIDENCE")
	private Publication publication;

	@JsonView({PublicView.Interaction.class})
	@Relationship(type = "SOURCE_DATABASE")
	private MITerm sourceDatabase;

	@JsonView({PublicView.Interaction.class})
	@Relationship(type = "AGGREGATION_DATABASE")
	private MITerm aggregationDatabase;

	@JsonView({PublicView.Interaction.class})
	@Relationship(type = "DETECTION_METHOD")
	private List<MITerm> detectionsMethods;

	@JsonView({PublicView.Interaction.class})
	@Relationship(type = "INTERACTION_TYPE")
	private MITerm interactionType;

	@JsonView({PublicView.Interaction.class})
	@Relationship(type = "INTERACTOR_A_TYPE")
	private MITerm interactorAType;

	@JsonView({PublicView.Interaction.class})
	@Relationship(type = "INTERACTOR_A_ROLE")
	private MITerm interactorARole;

	@JsonView({PublicView.Interaction.class})
	@Relationship(type = "INTERACTOR_B_TYPE")
	private MITerm interactorBType;

	@JsonView({PublicView.Interaction.class})
	@Relationship(type = "INTERACTOR_B_ROLE")
	private MITerm interactorBRole;

	@JsonView({PublicView.Interaction.class})
	@Relationship(type = "INTERACTOR_A_GENETIC_PERTURBATION")
	private Allele alleleA;

	@JsonView({PublicView.Interaction.class})
	@Relationship(type = "INTERACTOR_B_GENETIC_PERTURBATION")
	private Allele alleleB;
	
	@JsonView({PublicView.Interaction.class})
	@Relationship(type = "PHENOTYPE_TRAIT")
	private List<Phenotype> phenotypes = new ArrayList<>();

	@Override
	public String toString() {
		return geneA.getSymbol() + " : " + geneB.getSymbol();
	}

	@Override
	public int compareTo(Object o) {
		return 0;
	}
}
