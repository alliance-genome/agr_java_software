package org.alliancegenome.api.controller;

import java.util.Map;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import org.alliancegenome.api.rest.interfaces.DiseaseRESTInterface;
import org.alliancegenome.api.service.DiseaseService;
import org.jboss.logging.Logger;

@RequestScoped
public class DiseaseController implements DiseaseRESTInterface {

	private Logger log = Logger.getLogger(getClass());
	
	@Inject
	private DiseaseService diseaseService;
	
	@Override
	public Map<String, Object> getDisease(String id) {
		return diseaseService.getById(id);
	}

}
