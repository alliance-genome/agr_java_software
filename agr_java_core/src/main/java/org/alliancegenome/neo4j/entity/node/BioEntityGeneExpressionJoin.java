package org.alliancegenome.neo4j.entity.node;

import java.util.List;

import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import lombok.Getter;
import lombok.Setter;

@NodeEntity
@Getter
@Setter
public class BioEntityGeneExpressionJoin extends Association {

	@Relationship(type = "ASSOCIATION", direction = Relationship.INCOMING)
	private ExpressionBioEntity entity;

	@Relationship(type = "EVIDENCE")
	private List<Publication> publications;

	@Relationship(type = "DURING")
	private Stage stage;

	@Relationship(type = "STAGE_RIBBON_TERM")
	private UBERONTerm stageTerm;

	@Relationship(type = "ASSAY")
	private MMOTerm assay;

	@Relationship(type = "CROSS_REFERENCE")
	private List<CrossReference> crossReferences;

	@Relationship(type = "ASSOCIATION", direction = Relationship.INCOMING)
	private Gene gene;


}
