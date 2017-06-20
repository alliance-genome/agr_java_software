package org.alliancegenome.api.controller;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import org.alliancegenome.api.rest.GeneRESTInterface;
import org.alliancegenome.api.service.GeneService;

@RequestScoped
public class GeneController implements GeneRESTInterface {

	@Inject
	private GeneService geneService;

	@Override
	public void createGene(String api_access_token) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void createGeneBatch(String api_access_token) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateGene(String api_access_token) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void getGene(String primaryId, String symbol, String soTermId, String taxonId, String name) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void deleteGene(String api_access_token, Long id) {
		// TODO Auto-generated method stub
		
	}


}
