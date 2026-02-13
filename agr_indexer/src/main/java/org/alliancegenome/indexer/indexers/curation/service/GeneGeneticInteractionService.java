package org.alliancegenome.indexer.indexers.curation.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.alliancegenome.curation_api.model.entities.GeneGeneticInteraction;

public class GeneGeneticInteractionService extends BaseInteractionService {

	public List<GeneGeneticInteraction> getFilteredAndReversedInteractions(List<GeneGeneticInteraction> forwardInteractions) {

		List<GeneGeneticInteraction> validInteractions = new ArrayList<>();
		
		for (GeneGeneticInteraction interaction: forwardInteractions) {
			if (hasPerturbatingAllelesInNeo(interaction)) {
				if (hasInteractingGenesInNeo(interaction)) {
					if (hasNoObsoletedOrInternalEntities(interaction)) {
						validInteractions.add(interaction);
						try {
							GeneGeneticInteraction reverseInteraction = generateReverseInteraction(interaction);
							if (reverseInteraction != null) {
								validInteractions.add(reverseInteraction);
							}
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}

		return validInteractions;
	}

	private boolean hasPerturbatingAllelesInNeo(GeneGeneticInteraction interaction) {
		if (interaction.getInteractorAGeneticPerturbation() != null) {
			if (!isValidNeoEntity(getAllNeoAlleleIDs(), interaction.getInteractorAGeneticPerturbation().getIdentifier())) {
				return false;
			}
		}
		if (interaction.getInteractorBGeneticPerturbation() != null) {
			if (!isValidNeoEntity(getAllNeoAlleleIDs(), interaction.getInteractorBGeneticPerturbation().getIdentifier())) {
				return false;
			}
		}
		return true;
	}

	private GeneGeneticInteraction generateReverseInteraction(GeneGeneticInteraction forwardInteraction) throws IOException {
		GeneGeneticInteraction newInteraction = mapper.readValue(mapper.writeValueAsString(forwardInteraction), GeneGeneticInteraction.class);
		newInteraction.setInteractorAGeneticPerturbation(forwardInteraction.getInteractorBGeneticPerturbation());
		newInteraction.setInteractorBGeneticPerturbation(forwardInteraction.getInteractorAGeneticPerturbation());
		return reverseInteraction(forwardInteraction, newInteraction);
	}
	
	
}
