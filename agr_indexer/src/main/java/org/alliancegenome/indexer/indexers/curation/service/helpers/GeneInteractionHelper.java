package org.alliancegenome.indexer.indexers.curation.service.helpers;

import java.io.IOException;
import java.util.Objects;

import org.alliancegenome.curation_api.model.entities.GeneGeneticInteraction;
import org.alliancegenome.curation_api.model.entities.GeneInteraction;
import org.alliancegenome.curation_api.model.entities.GeneMolecularInteraction;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

public class GeneInteractionHelper  {

	
	private ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	
	public GeneGeneticInteraction generateReverseInteraction (GeneGeneticInteraction forwardInteraction) throws IOException {
		
		GeneGeneticInteraction newInteraction = objectMapper.readValue(objectMapper.writeValueAsString(forwardInteraction), GeneGeneticInteraction.class);
		newInteraction.setInteractorAGeneticPerturbation(forwardInteraction.getInteractorBGeneticPerturbation());
		newInteraction.setInteractorBGeneticPerturbation(forwardInteraction.getInteractorAGeneticPerturbation());
		return reverseInteraction(forwardInteraction, newInteraction);
	}
	
	public GeneMolecularInteraction generateReverseInteraction (GeneMolecularInteraction forwardInteraction) throws IOException {
		
		GeneMolecularInteraction newInteraction = objectMapper.readValue(objectMapper.writeValueAsString(forwardInteraction), GeneMolecularInteraction.class);
		return reverseInteraction(forwardInteraction, newInteraction);
	}
	

	private <E extends GeneInteraction> E reverseInteraction(E forwardInteraction, E reverseInteraction) {
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
