package org.alliancegenome.indexer.indexers.curation.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.curation_api.model.entities.GeneMolecularInteraction;
import org.alliancegenome.curation_api.response.SearchResponse;
import org.alliancegenome.indexer.RestConfig;
import org.alliancegenome.indexer.indexers.curation.interfaces.GeneMolecularInteractionInterface;

import si.mazi.rescu.RestProxyFactory;

public class GeneMolecularInteractionService extends BaseInteractionService {

	private GeneMolecularInteractionInterface geneMolecularInteractionApi = RestProxyFactory.createProxy(GeneMolecularInteractionInterface.class, ConfigHelper.getCurationApiUrl(), RestConfig.config);
	
	public SearchResponse<GeneMolecularInteraction> getGeneMolecularInteractions(Integer page, Integer limit) {
		HashMap<String, Object> params = new HashMap<>();
		params.put("internal", false);
		params.put("obsolete", false);
		
		return geneMolecularInteractionApi.findForPublic(page, limit, params);
	}
	
	public List<GeneMolecularInteraction> getFilteredAndReversedInteractions(List<GeneMolecularInteraction> forwardInteractions) {

		List<GeneMolecularInteraction> validInteractions = new ArrayList<>();
		
		for (GeneMolecularInteraction interaction: forwardInteractions) {
			if (hasInteractingGenesInNeo(interaction)) {
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
		}

		return validInteractions;
	}
	
	private GeneMolecularInteraction generateReverseInteraction(GeneMolecularInteraction forwardInteraction) throws IOException {
		GeneMolecularInteraction newInteraction = mapper.readValue(mapper.writeValueAsString(forwardInteraction), GeneMolecularInteraction.class);
		return reverseInteraction(forwardInteraction, newInteraction);
	}

}
