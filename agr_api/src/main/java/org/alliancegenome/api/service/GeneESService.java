package org.alliancegenome.api.service;

import org.alliancegenome.curation_api.model.document.es.GeneSummaryDocument;
import org.alliancegenome.api.es.dao.GeneESDAO;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

@RequestScoped
public class GeneESService extends ESService {

	@Inject
	GeneESDAO geneESDAO;

	public GeneSummaryDocument getById(String geneId) {
		return geneESDAO.getById(geneId);
	}
}
