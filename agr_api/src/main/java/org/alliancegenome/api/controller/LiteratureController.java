package org.alliancegenome.api.controller;

import java.util.List;
import java.util.Map;

import org.alliancegenome.api.es.query.Pagination;
import org.alliancegenome.api.exceptions.RestErrorException;
import org.alliancegenome.api.exceptions.RestErrorMessage;
import org.alliancegenome.api.response.JsonResultResponse;
import org.alliancegenome.api.rest.interfaces.LiteratureRESTInterface;
import org.alliancegenome.api.service.LiteratureESService;
import org.alliancegenome.api.service.ReferenceDataESService;
import org.alliancegenome.core.document.DiseaseAnnotationDocument;
import org.alliancegenome.core.document.GeneGeneticInteractionDocument;
import org.alliancegenome.core.document.GeneMolecularInteractionDocument;
import org.alliancegenome.core.document.LiteratureSummaryDocument;
import org.alliancegenome.core.document.PhenotypeAnnotationDocument;
import org.alliancegenome.curation_api.model.document.es.GeneExpressionDocument;

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
	public JsonResultResponse<DiseaseAnnotationDocument> getDiseaseAnnotationsByReference(
		String id,
		Integer limit,
		Integer page,
		String sortBy,
		String species,
		String diseaseName,
		String associationType,
		String evidenceCode,
		String dataProvider,
		String asc
	) {
		long startTime = System.currentTimeMillis();
		Pagination pagination = new Pagination(page, limit, sortBy, asc);
		pagination.addFilterOption("subject.taxon.species.fullName.keyword", species);
		pagination.addFilterOption("object.name", diseaseName);
		pagination.addFilterOption("generatedRelationString.keyword", associationType);
		pagination.addFilterOption("evidenceCodes.abbreviation", evidenceCode);
		pagination.addFilterOption("primaryAnnotations.dataProvider.abbreviation", dataProvider);
		validate(pagination);
		try {
			return timed(referenceDataESService.getDiseaseAnnotations(id, pagination), startTime);
		} catch (Exception e) {
			throw restError("Error while retrieving disease annotations by reference", e);
		}
	}

	@Override
	public JsonResultResponse<PhenotypeAnnotationDocument> getPhenotypeAnnotationsByReference(
		String id,
		Integer limit,
		Integer page,
		String sortBy,
		String species,
		String phenotype,
		String asc
	) {
		long startTime = System.currentTimeMillis();
		Pagination pagination = new Pagination(page, limit, sortBy, asc);
		pagination.addFilterOption("subject.taxon.species.fullName.keyword", species);
		pagination.addFilterOption("phenotypeStatement", phenotype);
		validate(pagination);
		try {
			return timed(referenceDataESService.getPhenotypeAnnotations(id, pagination), startTime);
		} catch (Exception e) {
			throw restError("Error while retrieving phenotype annotations by reference", e);
		}
	}

	@Override
	public JsonResultResponse<GeneExpressionDocument> getExpressionAnnotationsByReference(
		String id,
		List<String> crossReferences,
		Integer limit,
		Integer page,
		String sortBy,
		String species,
		String asc
	) {
		long startTime = System.currentTimeMillis();
		List<String> xrefs = splitCommas(crossReferences);
		requireCrossReferences(xrefs);
		Pagination pagination = new Pagination(page, limit, sortBy, asc);
		pagination.addFilterOption("geneExpressionAnnotation.expressionAnnotationSubject.taxon.species.fullName.keyword", species);
		validate(pagination);
		try {
			return timed(referenceDataESService.getExpressionAnnotations(xrefs, pagination), startTime);
		} catch (Exception e) {
			throw restError("Error while retrieving expression annotations by reference", e);
		}
	}

	@Override
	public JsonResultResponse<GeneMolecularInteractionDocument> getMolecularInteractionsByReference(
		String id,
		List<String> crossReferences,
		Integer limit,
		Integer page,
		String sortBy,
		String species,
		String asc
	) {
		long startTime = System.currentTimeMillis();
		List<String> xrefs = splitCommas(crossReferences);
		requireCrossReferences(xrefs);
		Pagination pagination = new Pagination(page, limit, sortBy, asc);
		pagination.addFilterOption("geneMolecularInteraction.geneAssociationSubject.taxon.species.fullName.keyword", species);
		validate(pagination);
		try {
			return timed(referenceDataESService.getMolecularInteractions(xrefs, pagination), startTime);
		} catch (Exception e) {
			throw restError("Error while retrieving molecular interactions by reference", e);
		}
	}

	@Override
	public JsonResultResponse<GeneGeneticInteractionDocument> getGeneticInteractionsByReference(
		String id,
		List<String> crossReferences,
		Integer limit,
		Integer page,
		String sortBy,
		String species,
		String asc
	) {
		long startTime = System.currentTimeMillis();
		List<String> xrefs = splitCommas(crossReferences);
		requireCrossReferences(xrefs);
		Pagination pagination = new Pagination(page, limit, sortBy, asc);
		pagination.addFilterOption("geneGeneticInteraction.geneAssociationSubject.taxon.species.fullName.keyword", species);
		validate(pagination);
		try {
			return timed(referenceDataESService.getGeneticInteractions(xrefs, pagination), startTime);
		} catch (Exception e) {
			throw restError("Error while retrieving genetic interactions by reference", e);
		}
	}

	@Override
	public JsonResultResponse<Map<String, Object>> getRelatedPapersByReference(String id, Integer limit, Boolean includeOrthologs) {
		long startTime = System.currentTimeMillis();
		try {
			int n = (limit == null || limit <= 0) ? 10 : limit;
			boolean expand = includeOrthologs != null && includeOrthologs;
			return timed(referenceDataESService.getRelatedPapers(id, n, expand), startTime);
		} catch (Exception e) {
			throw restError("Error while retrieving related papers", e);
		}
	}

	@Override
	public JsonResultResponse<Map<String, Object>> getOrthologyByReference(
		String id,
		Integer limit,
		Integer page,
		String sortBy,
		String asc
	) {
		long startTime = System.currentTimeMillis();
		Pagination pagination = new Pagination(page, limit, sortBy, asc);
		validate(pagination);
		try {
			return timed(referenceDataESService.getOrthologyByReference(id, pagination), startTime);
		} catch (Exception e) {
			throw restError("Error while retrieving orthology by reference", e);
		}
	}

	@Override
	public JsonResultResponse<Map<String, Object>> getGenesByReference(String id) {
		long startTime = System.currentTimeMillis();
		try {
			return timed(referenceDataESService.getGenesByReference(id), startTime);
		} catch (Exception e) {
			throw restError("Error while retrieving genes by reference", e);
		}
	}

	@Override
	public JsonResultResponse<Map<String, Object>> getAllelesByReference(String id) {
		long startTime = System.currentTimeMillis();
		try {
			return timed(referenceDataESService.getAllelesByReference(id), startTime);
		} catch (Exception e) {
			throw restError("Error while retrieving alleles by reference", e);
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

	// Expression / molecular-interaction / genetic-interaction docs on stage ES are
	// indexed by PMID/MOD curie (referenceId / evidence.referenceID), not by AGRKB curie.
	// The {id} path param can't be used to scope these queries, so the caller must pass
	// the cross-reference curies (typically pulled from the literature summary) as a
	// non-empty query param. Without it the endpoint would return unscoped data from
	// the entire index, so we reject the request with 400.
	private void requireCrossReferences(List<String> crossReferences) {
		if (crossReferences == null || crossReferences.isEmpty()) {
			RestErrorMessage message = new RestErrorMessage(
				"crossReferences query param is required (provide one or more PMID/MOD curies, comma-separated)");
			throw new RestErrorException(message);
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

	// JAX-RS gives a List<String> for repeated params; this also handles a single "a,b,c" value.
	private static List<String> splitCommas(List<String> raw) {
		if (raw == null) {
			return List.of();
		}
		return raw.stream()
			.filter(s -> s != null && !s.isBlank())
			.flatMap(s -> java.util.Arrays.stream(s.split(",")))
			.map(String::trim)
			.filter(s -> !s.isEmpty())
			.toList();
	}
}
