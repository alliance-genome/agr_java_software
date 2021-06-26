package org.alliancegenome.cache;

import org.alliancegenome.neo4j.entity.ConditionAnnotation;
import org.alliancegenome.neo4j.entity.PrimaryAnnotatedEntity;
import org.alliancegenome.neo4j.entity.node.CrossReference;
import org.alliancegenome.neo4j.entity.node.EntityJoin;
import org.alliancegenome.neo4j.entity.node.GeneticEntity;
import org.apache.commons.collections.CollectionUtils;

import java.util.List;

public class ConditionService {

    public static void populateExperimentalConditions(EntityJoin join, PrimaryAnnotatedEntity entity) {
        entity.addConditions(ConditionAnnotation.ConditionType.HAS_CONDITION, join.getHasConditionList());
        entity.addConditions(ConditionAnnotation.ConditionType.INDUCES, join.getInducerConditionList());
        entity.addModifier(ConditionAnnotation.ConditionType.AMELIORATES, join.getAmeliorateConditionList());
        entity.addModifier(ConditionAnnotation.ConditionType.EXACERBATES, join.getExacerbateConditionList());
    }

    public static PrimaryAnnotatedEntity createBaseLevelPAEs(EntityJoin entityJoin) {
        // create PAE from Allele when allele-level annotation or Gene when gene-level annotation,
        // i.e. no model / AGM or Allele off PublicationJoin node
        // needed for showing experimental conditions
        if (entityJoin.getPublicationJoins().stream().anyMatch(pubJoin -> CollectionUtils.isEmpty(pubJoin.getAlleles())
                && org.apache.commons.collections4.CollectionUtils.isEmpty(pubJoin.getModels()) && entityJoin.getModel() == null)
                && entityJoin.hasExperimentalConditions()) {
            GeneticEntity geneticEntity = entityJoin.getAllele();
            if (geneticEntity == null) {
                geneticEntity = entityJoin.getGene();
            }
            PrimaryAnnotatedEntity entity = new PrimaryAnnotatedEntity();
            entity.setId(geneticEntity.getPrimaryKey());
            entity.setName(geneticEntity.getSymbol());
            populateExperimentalConditions(entityJoin, entity);
            List<CrossReference> refs = geneticEntity.getCrossReferences();
            if (CollectionUtils.isNotEmpty(refs))
                entity.setUrl(refs.get(0).getCrossRefCompleteUrl());

            entity.setType(geneticEntity.getCrossReferenceType());
            entity.setDiseaseAssociationType(entityJoin.getJoinType());
            return entity;
        }
        return null;
    }


}
