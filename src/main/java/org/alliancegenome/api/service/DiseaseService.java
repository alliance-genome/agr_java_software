package org.alliancegenome.api.service;

import java.util.Map;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import org.alliancegenome.api.dao.DiseaseDAO;
import org.jboss.logging.Logger;

@RequestScoped
public class DiseaseService {
	
	private Logger log = Logger.getLogger(getClass());
	
	@Inject
	private DiseaseDAO diseaseDAO;

	public Map<String, Object> getById(String id) {
		return diseaseDAO.getById(id);
	}

	
}
