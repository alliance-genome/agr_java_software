package org.alliancegenome.api.controller;

import java.util.Map;

import org.alliancegenome.api.es.query.Pagination;
import org.alliancegenome.api.exceptions.RestErrorException;
import org.alliancegenome.api.exceptions.RestErrorMessage;
import org.alliancegenome.api.response.JsonResultResponse;
import org.alliancegenome.api.rest.interfaces.LiteratureRESTInterface;
import org.alliancegenome.api.service.LiteratureESService;
import org.alliancegenome.api.service.ReferenceDataESService;
import org.alliancegenome.core.document.LiteratureSummaryDocument;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

@RequestScoped
public class LiteratureController implements LiteratureRESTInterface {

	@Inject
	LiteratureESService literatureESService;

	@Inject
	ReferenceDataESService referenceDataESService;

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

	@Override
	public JsonResultResponse<Map<String, Object>> getGenesByReference(String id, Integer limit, Integer page) {
		long startTime = System.currentTimeMillis();
		Pagination pagination = new Pagination(page, limit, null, null);
		validate(pagination);
		try {
			return timed(referenceDataESService.getGenesByReference(id, pagination), startTime);
		} catch (Exception e) {
			throw restError("Error while retrieving genes by reference", e);
		}
	}

	@Override
	public JsonResultResponse<Map<String, Object>> getAllelesByReference(String id, Integer limit, Integer page) {
		long startTime = System.currentTimeMillis();
		Pagination pagination = new Pagination(page, limit, null, null);
		validate(pagination);
		try {
			return timed(referenceDataESService.getAllelesByReference(id, pagination), startTime);
		} catch (Exception e) {
			throw restError("Error while retrieving alleles by reference", e);
		}
	}

	@Override
	public JsonResultResponse<Map<String, Object>> getTransgenicAllelesByReference(String id, Integer limit, Integer page) {
		long startTime = System.currentTimeMillis();
		Pagination pagination = new Pagination(page, limit, null, null);
		validate(pagination);
		try {
			return timed(referenceDataESService.getTransgenicAllelesByReference(id, pagination), startTime);
		} catch (Exception e) {
			throw restError("Error while retrieving transgenic alleles by reference", e);
		}
	}

	@Override
	public JsonResultResponse<Map<String, Object>> getModelsByReference(String id) {
		long startTime = System.currentTimeMillis();
		try {
			return timed(referenceDataESService.getModelsByReference(id), startTime);
		} catch (Exception e) {
			throw restError("Error while retrieving models by reference", e);
		}
	}

	private void validate(Pagination pagination) {
		if (pagination.hasErrors()) {
			RestErrorMessage message = new RestErrorMessage();
			message.setErrors(pagination.getErrors());
			throw new RestErrorException(message);
		}
	}

	private <T> JsonResultResponse<T> timed(JsonResultResponse<T> response, long startTime) {
		response.setHttpServletRequest(null);
		response.calculateRequestDuration(startTime);
		return response;
	}

	private RestErrorException restError(String logMsg, Exception e) {
		Log.error(logMsg, e);
		RestErrorMessage error = new RestErrorMessage();
		error.addErrorMessage(e.getMessage());
		return new RestErrorException(error);
	}
}
