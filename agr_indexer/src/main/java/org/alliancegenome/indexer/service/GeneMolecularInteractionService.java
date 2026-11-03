package org.alliancegenome.indexer.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.alliancegenome.curation_api.model.entities.GeneMolecularInteraction;

public class GeneMolecularInteractionService extends BaseInteractionService {

	public List<GeneMolecularInteraction> getFilteredAndReversedInteractions(List<GeneMolecularInteraction> forwardInteractions) {

		List<GeneMolecularInteraction> validInteractions = new ArrayList<>();
		
		for (GeneMolecularInteraction interaction: forwardInteractions) {
			if (hasNoObsoletedOrInternalEntities(interaction)) {
				validInteractions.add(interaction);
				try {
					GeneMolecularInteraction reverseInteraction = generateReverseInteraction(interaction);
					if (reverseInteraction != null) {
						validInteractions.add(reverseInteraction);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		return validInteractions;
	}
	
	private GeneMolecularInteraction generateReverseInteraction(GeneMolecularInteraction forwardInteraction) throws IOException {
		GeneMolecularInteraction newInteraction = mapper.readValue(mapper.writeValueAsString(forwardInteraction), GeneMolecularInteraction.class);
		return reverseInteraction(forwardInteraction, newInteraction);
	}

}
