package org.alliancegenome.indexer.indexers.curation.service.helpers;

import java.io.IOException;
import java.util.Objects;

import org.alliancegenome.curation_api.model.entities.GeneGeneticInteraction;
import org.alliancegenome.curation_api.model.entities.GeneInteraction;
import org.alliancegenome.curation_api.model.entities.GeneMolecularInteraction;
import org.alliancegenome.indexer.RestConfig;

import com.fasterxml.jackson.databind.ObjectMapper;

public class GeneInteractionHelper {

	private ObjectMapper mapper = RestConfig.config.getJacksonObjectMapperFactory().createObjectMapper();
	
	public GeneGeneticInteraction generateReverseInteraction(GeneGeneticInteraction forwardInteraction) throws IOException {
		GeneGeneticInteraction newInteraction = mapper.readValue(mapper.writeValueAsString(forwardInteraction), GeneGeneticInteraction.class);
		newInteraction.setInteractorAGeneticPerturbation(forwardInteraction.getInteractorBGeneticPerturbation());
		newInteraction.setInteractorBGeneticPerturbation(forwardInteraction.getInteractorAGeneticPerturbation());
		return reverseInteraction(forwardInteraction, newInteraction);
	}
	
	public GeneMolecularInteraction generateReverseInteraction(GeneMolecularInteraction forwardInteraction) throws IOException {
		GeneMolecularInteraction newInteraction = mapper.readValue(mapper.writeValueAsString(forwardInteraction), GeneMolecularInteraction.class);
		return reverseInteraction(forwardInteraction, newInteraction);
	}
	

	private <E extends GeneInteraction> E reverseInteraction(E forwardInteraction, E reverseInteraction) {
		if(forwardInteraction.getGeneAssociationSubject() == null || forwardInteraction.getGeneGeneAssociationObject() == null) {
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
