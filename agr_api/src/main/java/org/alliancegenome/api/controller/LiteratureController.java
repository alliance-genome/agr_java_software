package org.alliancegenome.api.controller;

import org.alliancegenome.api.entity.LiteratureSummaryDocument;
import org.alliancegenome.api.rest.interfaces.LiteratureRESTInterface;
import org.alliancegenome.api.service.LiteratureESService;
import org.alliancegenome.core.exceptions.RestErrorException;
import org.alliancegenome.core.exceptions.RestErrorMessage;

import jakarta.inject.Inject;

public class LiteratureController implements LiteratureRESTInterface {
	@Inject
	LiteratureESService literatureESService;
	
	@Override
	public LiteratureSummaryDocument getLiterature(String id) {
		LiteratureSummaryDocument literatureSummary = literatureESService.getById(id);
		if (literatureSummary == null) {
			RestErrorMessage error = new RestErrorMessage("No literature summary found with ID: " + id);
			throw new RestErrorException(error);
		} else {
			return literatureSummary;
		}
	}
}
