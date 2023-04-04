package org.alliancegenome.core.util;

import org.alliancegenome.neo4j.entity.DiseaseAnnotation;
import org.alliancegenome.neo4j.entity.PrimaryAnnotatedEntity;
import org.alliancegenome.neo4j.entity.node.AffectedGenomicModel;

public class ModelHelper {

	public static PrimaryAnnotatedEntity getPrimaryAnnotatedEntity(DiseaseAnnotation annotation) {
		AffectedGenomicModel model = annotation.getModel();
		PrimaryAnnotatedEntity entity = new PrimaryAnnotatedEntity();
		entity.setId(model.getPrimaryKey());
		entity.setEntityJoinPk(annotation.getPrimaryKey());
		entity.setName(model.getName());
		entity.setDisplayName(model.getNameText());
		entity.setUrl(model.getModCrossRefCompleteUrl());
		entity.setType(model.getSubtype());
		entity.addPublicationEvidenceCode(annotation.getPublicationJoins());
		entity.addDisease(annotation.getDisease(), annotation.getAssociationType());
		entity.setDataProvider(model.getDataProvider());
		entity.setSpecies(model.getSpecies());
		return entity;
	}
}
