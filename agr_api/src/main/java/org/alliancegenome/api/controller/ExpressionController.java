package org.alliancegenome.api.controller;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.alliancegenome.api.dto.RibbonSummary;
import org.alliancegenome.api.rest.interfaces.ExpressionRESTInterface;
import org.alliancegenome.api.service.EntityType;
import org.alliancegenome.api.service.ExpressionESService;
import org.alliancegenome.api.service.ExpressionRibbonESService;
import org.alliancegenome.api.service.helper.APIServiceHelper;
import org.alliancegenome.cache.repository.helper.JsonResultResponse;
import org.alliancegenome.core.exceptions.RestErrorException;
import org.alliancegenome.core.exceptions.RestErrorMessage;
import org.alliancegenome.api.translators.tdf.ExpressionToTdfTranslator;
import org.alliancegenome.curation_api.model.document.es.GeneExpressionDocument;
import org.alliancegenome.es.model.query.Pagination;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

@RequestScoped
public class ExpressionController implements ExpressionRESTInterface {

	@Inject ExpressionESService expressionESService;
	@Inject ExpressionRibbonESService expressionRibbonESService;

	private static final ExpressionToTdfTranslator expressionTranslator = new ExpressionToTdfTranslator();

	@Override
	public JsonResultResponse<GeneExpressionDocument> getExpressionAnnotations(
																		String termID,
																		String focusTaxonId,
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
																		String asc,
																		List<String> geneIDs) {

		LocalDateTime startDate = LocalDateTime.now();
		try {
			JsonResultResponse<GeneExpressionDocument> response = getExpressionDetailJsonResultResponse(
					geneIDs,
					termID,
					focusTaxonId,
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

	private JsonResultResponse<GeneExpressionDocument> getExpressionDetailJsonResultResponse(List<String> geneIDs, String termID, String focusTaxonId, String filterSpecies, String filterGene, String filterStage, String filterAssay, String filterReference, String filterLocation, String filterSource, Integer limit, Integer page, String sortBy, String asc) {
		long startTime = System.currentTimeMillis();
		Pagination pagination = new Pagination(page, limit, sortBy, asc);
		pagination.addFilterOption("geneExpressionAnnotation.expressionAnnotationSubject.taxon.species.fullName.keyword", filterSpecies);
		pagination.addFilterOption("geneExpressionAnnotation.expressionAnnotationSubject.geneSymbol.displayText", filterGene);
		pagination.addFilterOption("geneExpressionAnnotation.whereExpressedStatement", filterLocation);
		pagination.addFilterOption("geneExpressionAnnotation.whenExpressedStageName", filterStage);
		pagination.addFilterOption("geneExpressionAnnotation.expressionAssayUsed.name", filterAssay);
		pagination.addFilterOption("geneExpressionAnnotation.crossReferences.referencedCurie", filterSource);
		pagination.addFilterOption("referenceId", filterReference);

		JsonResultResponse<GeneExpressionDocument> expressions = expressionESService.getExpressionAnnotations(geneIDs, termID, focusTaxonId, pagination);
		expressions.calculateRequestDuration(startTime);
		return expressions;

	}

	@Override
	public RibbonSummary getExpressionSummary(List<String> geneIDs) {
		List<String> ids = new ArrayList<>();
		if (geneIDs != null) {
			ids.addAll(geneIDs);
		}

		try {
			return expressionRibbonESService.getExpressionRibbonSummary(ids);
		} catch (Exception e) {
			Log.error("error", e);
			RestErrorMessage error = new RestErrorMessage();
			error.addErrorMessage(e.getMessage());
			throw new RestErrorException(error);
		}
	}

	@Override
	public Response getExpressionAnnotationsDownload(String termID,
													String focusTaxonId,
													String filterSpecies,
													String filterGene,
													String filterStage,
													String filterAssay,
													String filterReference,
													String filterLocation,
													String filterSource,
													String sortBy,
													String asc,
													List<String> geneIDs
													) {

		JsonResultResponse<GeneExpressionDocument> result = getExpressionDetailJsonResultResponse(
				geneIDs,
				termID,
				focusTaxonId,
				filterSpecies,
				filterGene,
				filterStage,
				filterAssay,
				filterReference,
				filterLocation,
				filterSource,
				200000,
				1,
				sortBy,
				asc);

		Response.ResponseBuilder responseBuilder = Response.ok(expressionTranslator.getAllRows(result.getResults(), geneIDs.size() > 1));
		APIServiceHelper.setDownloadHeader(geneIDs.get(0), EntityType.GENE, EntityType.EXPRESSION, responseBuilder);
		return responseBuilder.build();
	}

}
