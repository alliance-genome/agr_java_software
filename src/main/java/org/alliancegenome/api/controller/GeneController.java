package org.alliancegenome.api.controller;

import java.util.Map;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import org.alliancegenome.api.rest.interfaces.GeneRESTInterface;
import org.alliancegenome.api.service.GeneService;

@RequestScoped
public class GeneController implements GeneRESTInterface {

	@Inject
	private GeneService geneService;
	
	@Override
	public Map<String, Object> getGene(String id) {
		return geneService.getById(id);
	}

}
