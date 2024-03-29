package org.alliancegenome.cache;

import java.util.List;

import org.alliancegenome.neo4j.entity.ConditionAnnotation;
import org.alliancegenome.neo4j.entity.PrimaryAnnotatedEntity;
import org.alliancegenome.neo4j.entity.node.CrossReference;
import org.alliancegenome.neo4j.entity.node.EntityJoin;
import org.alliancegenome.neo4j.entity.node.GeneticEntity;
import org.apache.commons.collections.CollectionUtils;

public class ConditionService {

	public static void populateExperimentalConditions(EntityJoin join, PrimaryAnnotatedEntity entity) {
		entity.addCondition(ConditionAnnotation.ConditionType.HAS_CONDITION, join.getHasConditionList());
		entity.addCondition(ConditionAnnotation.ConditionType.INDUCES, join.getInducerConditionList());
		entity.addModifier(ConditionAnnotation.ConditionType.AMELIORATES, join.getAmeliorateConditionList());
		entity.addModifier(ConditionAnnotation.ConditionType.EXACERBATES, join.getExacerbateConditionList());
	}

	public static PrimaryAnnotatedEntity createBaseLevelPAEs(EntityJoin entityJoin) {
		// create PAE from Allele when allele-level annotation or Gene when gene-level annotation,
		// i.e. no model / AGM or Allele off PublicationJoin node
		// needed for showing experimental conditions
		if (entityJoin.getPublicationJoins().stream().anyMatch(pubJoin -> CollectionUtils.isEmpty(pubJoin.getAlleles())
				&& CollectionUtils.isEmpty(pubJoin.getModels()) && entityJoin.getModel() == null)) {
			GeneticEntity geneticEntity = entityJoin.getAllele();
			if (geneticEntity == null) {
				geneticEntity = entityJoin.getGene();
			}
			PrimaryAnnotatedEntity entity = new PrimaryAnnotatedEntity();
			entity.setId(geneticEntity.getPrimaryKey());
			entity.setName(geneticEntity.getSymbol());
			if (entityJoin.hasExperimentalConditions()) {
				populateExperimentalConditions(entityJoin, entity);
			}
			List<CrossReference> refs = geneticEntity.getCrossReferences();
			if (CollectionUtils.isNotEmpty(refs))
				entity.setUrl(refs.get(0).getCrossRefCompleteUrl());

			entity.addPublicationEvidenceCode(entityJoin.getPublicationJoins());
			entity.setType(geneticEntity.getCrossReferenceType().getDisplayName());
			entity.setDiseaseAssociationType(entityJoin.getJoinType());
			return entity;
		}
		return null;
	}


}
