package org.alliancegenome.neo4j.entity.node;

import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.neo4j.ogm.annotation.*;

import lombok.*;

@NodeEntity
@Getter
@Setter
public class EntityJoin extends Association {

	protected String primaryKey;
	protected String joinType;

	@Relationship(type = "EVIDENCE", direction = Relationship.INCOMING)
	private List<Publication> publications;

	@Relationship(type = "EVIDENCE")
	private List<ECOTerm> evidenceCodes;

	@Relationship(type = "ASSOCIATION", direction = Relationship.INCOMING)
	private Gene gene;

	@Relationship(type = "ASSOCIATION", direction = Relationship.INCOMING)
	private Allele allele;

	// direct annotations
	@Relationship(type = "ASSOCIATION", direction = Relationship.INCOMING)
	private AffectedGenomicModel model;

	@Relationship(type = "EVIDENCE")
	protected List<PublicationJoin> publicationJoins;

	@Relationship(type = "INDUCES")
	private List<ExperimentalCondition> inducerConditionList;

	@Relationship(type = "HAS_CONDITION")
	private List<ExperimentalCondition> hasConditionList;

	@Relationship(type = "AMELIORATES")
	private List<ExperimentalCondition> ameliorateConditionList;

	@Relationship(type = "EXACERBATES")
	private List<ExperimentalCondition> exacerbateConditionList;

	public boolean hasExperimentalConditions() {
		return CollectionUtils.isNotEmpty(inducerConditionList) ||
				CollectionUtils.isNotEmpty(hasConditionList) ||
				CollectionUtils.isNotEmpty(ameliorateConditionList) ||
				CollectionUtils.isNotEmpty(exacerbateConditionList);
	}
}
