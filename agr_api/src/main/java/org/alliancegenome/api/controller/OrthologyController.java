package org.alliancegenome.api.controller;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import org.alliancegenome.api.rest.interfaces.OrthologyRESTInterface;
import org.alliancegenome.cache.repository.helper.JsonResultResponse;
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

	@Inject GeneController controller;

	private static OrthologousRepository orthoRepo = new OrthologousRepository();

	@Override
	public JsonResultResponse<HomologView> getMultiSpeciesOrthology(List<String> taxonID, String taxonIdList, String stringencyFilter, String methods, Integer rows, Integer start) throws IOException {
		JsonResultResponse<HomologView> response = new JsonResultResponse<HomologView>();
		response.setNote("Not yet implemented");
		response.setApiVersion(API_VERSION);
		return response;
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
