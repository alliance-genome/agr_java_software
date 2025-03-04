package org.alliancegenome.indexer.indexers.curation.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.alliancegenome.curation_api.model.entities.GeneGeneticInteraction;
import org.alliancegenome.curation_api.model.entities.GeneInteraction;
import org.alliancegenome.curation_api.model.entities.base.AuditedObject;
import org.alliancegenome.indexer.RestConfig;

import com.fasterxml.jackson.databind.ObjectMapper;

public class BaseInteractionService extends BaseService {

	protected ObjectMapper mapper = RestConfig.config.getJacksonObjectMapperFactory().createObjectMapper();
	
	protected <E extends GeneInteraction> boolean hasNoObsoletedOrInternalEntities(E interaction) {
		List<AuditedObject> entitiesToBeValidated = new ArrayList<>();
		
		if (interaction instanceof GeneGeneticInteraction geneticInteraction) {
			if (geneticInteraction.getInteractorAGeneticPerturbation() != null) {
				entitiesToBeValidated.add(geneticInteraction.getInteractorAGeneticPerturbation());
			}
			if (geneticInteraction.getInteractorBGeneticPerturbation() != null) {
				entitiesToBeValidated.add(geneticInteraction.getInteractorBGeneticPerturbation());
			}
		}
		
		if (interaction.getGeneAssociationSubject() != null) {
			entitiesToBeValidated.add(interaction.getGeneAssociationSubject());
		}
		
		if (interaction.getGeneGeneAssociationObject() != null) {
			entitiesToBeValidated.add(interaction.getGeneGeneAssociationObject());
		}
		
		return hasNoExcludedEntities(entitiesToBeValidated);
	}

	protected <E extends GeneInteraction> boolean hasInteractingGenesInNeo(E interaction) {
		if (interaction.getGeneAssociationSubject() != null) {
			if (!isValidNeoEntity(getAllNeoGeneIDs(), interaction.getGeneAssociationSubject().getIdentifier())) {
				return false;
			}
		}
		if (interaction.getGeneGeneAssociationObject() != null) {
			if (!isValidNeoEntity(getAllNeoGeneIDs(), interaction.getGeneGeneAssociationObject().getIdentifier())) {
				return false;
			}
		}
		return true;
	}

	protected <E extends GeneInteraction> E reverseInteraction(E forwardInteraction, E reverseInteraction) {
		if (forwardInteraction.getGeneAssociationSubject() == null || forwardInteraction.getGeneGeneAssociationObject() == null) {
			return null;
		}
		
		if (Objects.equals(forwardInteraction.getGeneAssociationSubject().getIdentifier(), forwardInteraction.getGeneGeneAssociationObject().getIdentifier())) {
			return null;
		}
		
		reverseInteraction.setGeneAssociationSubject(forwardInteraction.getGeneGeneAssociationObject());
		reverseInteraction.setGeneGeneAssociationObject(forwardInteraction.getGeneAssociationSubject());
		
		reverseInteraction.setInteractorARole(forwardInteraction.getInteractorBRole());
		reverseInteraction.setInteractorBRole(forwardInteraction.getInteractorARole());
		
		reverseInteraction.setInteractorAType(forwardInteraction.getInteractorBType());
		reverseInteraction.setInteractorBType(forwardInteraction.getInteractorAType());
		
		return reverseInteraction;
	}
}
