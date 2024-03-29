package org.alliancegenome.api.controller;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import org.alliancegenome.api.rest.interfaces.OrthologyRESTInterface;
import org.alliancegenome.cache.repository.OrthologyCacheRepository;
import org.alliancegenome.cache.repository.helper.JsonResultResponse;
import org.alliancegenome.core.exceptions.RestErrorException;
import org.alliancegenome.core.exceptions.RestErrorMessage;
import org.alliancegenome.es.model.query.FieldFilter;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.neo4j.entity.node.OrthoAlgorithm;
import org.alliancegenome.neo4j.repository.OrthologousRepository;
import org.alliancegenome.neo4j.view.HomologView;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

@RequestScoped
public class OrthologyController implements OrthologyRESTInterface {

	public static final String API_VERSION = "0.91";

	// @Context
	// private HttpRequest request;

	@Inject OrthologyCacheRepository service;

	@Inject GeneController controller;

	private static OrthologousRepository orthoRepo = new OrthologousRepository();

	@Override
	public JsonResultResponse<HomologView> getDoubleSpeciesOrthology(String taxonIDOne, String taxonIDTwo, String stringency, String method, Integer limit, Integer page) {

		LocalDateTime startDate = LocalDateTime.now();
		Pagination pagination = new Pagination(page, limit, null, null);
		pagination.addFieldFilter(FieldFilter.STRINGENCY, stringency);
		pagination.addFieldFilter(FieldFilter.ORTHOLOGY_METHOD, method);
		if (pagination.hasErrors()) {
			RestErrorMessage message = new RestErrorMessage();
			message.setErrors(pagination.getErrors());
			throw new RestErrorException(message);
		}

		JsonResultResponse<HomologView> response = service.getOrthologyByTwoSpecies(taxonIDOne, taxonIDTwo, pagination);
		response.calculateRequestDuration(startDate);
		response.setApiVersion(API_VERSION);
		response.setHttpServletRequest(null);
		return response;
	}

	@Override
	public JsonResultResponse<HomologView> getSingleSpeciesOrthology(String species, String stringencyFilter, String methods, Integer limit, Integer page) {
		LocalDateTime startDate = LocalDateTime.now();
		Pagination pagination = new Pagination(page, limit, null, null);
		pagination.addFieldFilter(FieldFilter.STRINGENCY, stringencyFilter);
		pagination.addFieldFilter(FieldFilter.ORTHOLOGY_METHOD, methods);
		if (pagination.hasErrors()) {
			RestErrorMessage message = new RestErrorMessage();
			message.setErrors(pagination.getErrors());
			throw new RestErrorException(message);
		}

		JsonResultResponse<HomologView> response = service.getOrthologyBySpecies(species, pagination);
		response.calculateRequestDuration(startDate);
		response.setApiVersion(API_VERSION);
		response.setHttpServletRequest(null);
		return response;
	}

	@Override
	public JsonResultResponse<HomologView> getMultiSpeciesOrthology(List<String> taxonID, String taxonIdList, String stringencyFilter, String methods, Integer rows, Integer start) throws IOException {
		JsonResultResponse<HomologView> response = new JsonResultResponse<HomologView>();
		response.setNote("Not yet implemented");
		response.setApiVersion(API_VERSION);
		return response;
	}

	@Override
	public JsonResultResponse<HomologView> getMultiGeneOrthology(List<String> geneIDs, String geneList, String stringencyFilter, String method, Integer rows, Integer page) {
		// controller.setRequest(request);
		return controller.getGeneOrthology(null, geneIDs, geneList, stringencyFilter, null, method, rows, page);
	}

	@Override
	public JsonResultResponse<OrthoAlgorithm> getAllMethodsCalculations() {
		LocalDateTime startDate = LocalDateTime.now();
		JsonResultResponse<OrthoAlgorithm> response = new JsonResultResponse<>();
		List<OrthoAlgorithm> methodList = orthoRepo.getAllMethods();
		response.setResults(methodList);
		response.setTotal(methodList.size());
		response.setApiVersion(API_VERSION);
		response.calculateRequestDuration(startDate);
		return response;
	}
}
