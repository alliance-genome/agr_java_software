package org.alliancegenome.indexer.indexers.curation.service.helpers;

import java.io.IOException;
import java.util.Objects;

import org.alliancegenome.curation_api.model.entities.GeneGeneticInteraction;
import org.alliancegenome.curation_api.model.entities.GeneInteraction;
import org.alliancegenome.curation_api.model.entities.GeneMolecularInteraction;

public class GeneInteractionHelper {

	public GeneGeneticInteraction generateReverseInteraction(GeneGeneticInteraction forwardInteraction) throws IOException {
		
		GeneGeneticInteraction newInteraction = new GeneGeneticInteraction();
		newInteraction.setInteractorAGeneticPerturbation(forwardInteraction.getInteractorBGeneticPerturbation());
		newInteraction.setInteractorBGeneticPerturbation(forwardInteraction.getInteractorAGeneticPerturbation());
		return reverseInteraction(forwardInteraction, newInteraction);
	}
	
	public GeneMolecularInteraction generateReverseInteraction(GeneMolecularInteraction forwardInteraction) {
		
		GeneMolecularInteraction newInteraction = new GeneMolecularInteraction();
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
		
		reverseInteraction.setInteractionId(forwardInteraction.getInteractionId());
		reverseInteraction.setInteractionSource(forwardInteraction.getInteractionSource());
		reverseInteraction.setInteractionType(forwardInteraction.getInteractionType());
		reverseInteraction.setCrossReferences(forwardInteraction.getCrossReferences());

		reverseInteraction.setRelation(forwardInteraction.getRelation());
		
		reverseInteraction.setEvidence(forwardInteraction.getEvidence());

		reverseInteraction.setDateCreated(forwardInteraction.getDateCreated());
		reverseInteraction.setInternal(forwardInteraction.getInternal());
		reverseInteraction.setObsolete(forwardInteraction.getObsolete());
		
		return reverseInteraction;
	}
}
