package org.alliancegenome.api.controller;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.alliancegenome.api.dto.RibbonSummary;
import org.alliancegenome.api.rest.interfaces.ExpressionRESTInterface;
import org.alliancegenome.api.service.EntityType;
import org.alliancegenome.api.service.ExpressionService;
import org.alliancegenome.api.service.GeneService;
import org.alliancegenome.api.service.helper.APIServiceHelper;
import org.alliancegenome.cache.repository.helper.JsonResultResponse;
import org.alliancegenome.core.ExpressionDetail;
import org.alliancegenome.core.exceptions.RestErrorException;
import org.alliancegenome.core.exceptions.RestErrorMessage;
import org.alliancegenome.core.translators.tdf.ExpressionToTdfTranslator;
import org.alliancegenome.es.model.query.FieldFilter;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.neo4j.entity.SpeciesType;
import org.alliancegenome.neo4j.entity.node.BioEntityGeneExpressionJoin;
import org.alliancegenome.neo4j.view.BaseFilter;
import org.alliancegenome.neo4j.view.View;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

@RequestScoped
public class ExpressionController implements ExpressionRESTInterface {

	//@Context
	//private HttpRequest request;

	@Inject ExpressionService expressionService;

	@Inject GeneService geneService;
	
	private static final ExpressionToTdfTranslator expressionTranslator = new ExpressionToTdfTranslator();

	@Override
	public JsonResultResponse<ExpressionDetail> getExpressionAnnotations(List<String> geneIDs,
																		 String termID,
																		 String filterSpecies,
																		 String filterGene,
																		 String filterStage,
																		 String filterAssay,
																		 String filterReference,
																		 String filterTerm,
																		 String filterSource,
																		 Integer limit,
																		 Integer page,
																		 String sortBy,
																		 String asc) {

		LocalDateTime startDate = LocalDateTime.now();
		try {
			JsonResultResponse<ExpressionDetail> response = getExpressionDetailJsonResultResponse(
					geneIDs,
					termID,
					filterSpecies,
					filterGene,
					filterStage,
					filterAssay,
					filterReference,
					filterTerm,
					filterSource,
					limit,
					page,
					sortBy,
					asc);
			response.calculateRequestDuration(startDate);
			response.setHttpServletRequest(null);
			return response;
		} catch (Exception e) {
			Log.error("Error while retrieving expression data", e);
			RestErrorMessage error = new RestErrorMessage();
			error.addErrorMessage(e.getMessage());
			throw new RestErrorException(error);
		}
	}

	private JsonResultResponse<ExpressionDetail> getExpressionDetailJsonResultResponse(List<String> geneIDs, String termID, String filterSpecies, String filterGene, String filterStage, String filterAssay, String filterReference, String filterTerm, String filterSource, Integer limit, Integer page, String sortBy, String asc) {
		long startTime = System.currentTimeMillis();
		Pagination pagination = new Pagination(page, limit, sortBy, asc);
		BaseFilter filterMap = new BaseFilter();
		filterMap.put(FieldFilter.FSPECIES, filterSpecies);
		filterMap.put(FieldFilter.GENE_NAME, filterGene);
		filterMap.put(FieldFilter.FREFERENCE, filterReference);
		filterMap.put(FieldFilter.SOURCE, filterSource);
		filterMap.put(FieldFilter.TERM_NAME, filterTerm);
		filterMap.put(FieldFilter.ASSAY, filterAssay);
		filterMap.put(FieldFilter.STAGE, filterStage);
		filterMap.values().removeIf(Objects::isNull);
		pagination.setFieldFilterValueMap(filterMap);

		JsonResultResponse<ExpressionDetail> expressions = expressionService.getExpressionDetails(geneIDs, termID, pagination);
		expressions.calculateRequestDuration(startTime);
		return expressions;

	}

	@Override
	public String getExpressionAnnotationsByTaxon(String species,
												  String termID,
												  Integer limit,
												  Integer page) throws JsonProcessingException {
		Pagination pagination = new Pagination(page, limit, null, null);
		BaseFilter filterMap = new BaseFilter();
		filterMap.put(FieldFilter.TERM_NAME, termID);
		filterMap.values().removeIf(Objects::isNull);
		pagination.setFieldFilterValueMap(filterMap);

		LocalDateTime startDate = LocalDateTime.now();
		JsonResultResponse<ExpressionDetail> response = new JsonResultResponse<>();
		response.setHttpServletRequest(null);

		ObjectMapper mapper = new ObjectMapper();
		mapper.disable(MapperFeature.DEFAULT_VIEW_INCLUSION);

		// check if valid taxon identifier
		String taxon = SpeciesType.getTaxonId(species);

		List<BioEntityGeneExpressionJoin> joins = geneService.getExpressionAnnotationsByTaxon(taxon, termID, pagination);

		JsonResultResponse<ExpressionDetail> result = expressionService.getExpressionDetails(joins, pagination);
		response.setResults(result.getResults());
		response.setTotal(result.getTotal());
		response.calculateRequestDuration(startDate);
		return mapper.writerWithView(View.Expression.class).writeValueAsString(response);
	}

	@Override
	public RibbonSummary getExpressionSummary(List<String> geneIDs) {
		List<String> ids = new ArrayList<>();
		if (geneIDs != null)
			ids.addAll(geneIDs);

		try {
			return expressionService.getExpressionRibbonSummary(ids);
		} catch (Exception e) {
			Log.error("error", e);
			RestErrorMessage error = new RestErrorMessage();
			error.addErrorMessage(e.getMessage());
			throw new RestErrorException(error);
		}
	}

	@Override
	public Response getExpressionAnnotationsDownload(List<String> geneIDs,
													 String termID,
													 String filterSpecies,
													 String filterGene,
													 String filterStage,
													 String filterAssay,
													 String filterReference,
													 String filterTerm,
													 String filterSource,
													 String sortBy,
													 String asc) {

		JsonResultResponse<ExpressionDetail> result = getExpressionDetailJsonResultResponse(
				geneIDs,
				termID,
				filterSpecies,
				filterGene,
				filterStage,
				filterAssay,
				filterReference,
				filterTerm,
				filterSource,
				Integer.MAX_VALUE,
				1,
				sortBy,
				asc);

		Response.ResponseBuilder responseBuilder = Response.ok(expressionTranslator.getAllRows(result.getResults(), geneIDs.size() > 1));
		APIServiceHelper.setDownloadHeader(geneIDs.get(0), EntityType.GENE, EntityType.EXPRESSION, responseBuilder);
		return responseBuilder.build();
	}

}
