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
		String gene,
		String allele,
		String diseaseName,
		String associationType,
		String diseaseQualifier,
		String evidenceCode,
		String basedOnGene,
		String dataProvider,
		String reference,
		String asc
	) {
		long startTime = System.currentTimeMillis();
		Pagination pagination = new Pagination(page, limit, sortBy, asc);
		pagination.addFilterOption("subject.taxon.species.fullName.keyword", species);
		pagination.addFilterOption("primaryAnnotations.inferredGene.geneSymbol.displayText", gene);
		pagination.addFilterOption("primaryAnnotations.inferredAllele.alleleSymbol.displayText", allele);
		pagination.addFilterOption("object.name", diseaseName);
		pagination.addFilterOption("generatedRelationString.keyword", associationType);
		pagination.addFilterOption("diseaseQualifiers.keyword", diseaseQualifier);
		pagination.addFilterOption("evidenceCodes.abbreviation", evidenceCode);
		pagination.addFilterOption("primaryAnnotations.with.geneSymbol.displayText", basedOnGene);
		pagination.addFilterOption("primaryAnnotations.dataProvider.abbreviation", dataProvider);
		pagination.addFilterOption("pubmedPubModIDs", reference);
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
		String gene,
		String allele,
		String phenotype,
		String dataProvider,
		String reference,
		String asc
	) {
		long startTime = System.currentTimeMillis();
		Pagination pagination = new Pagination(page, limit, sortBy, asc);
		pagination.addFilterOption("subject.taxon.species.fullName.keyword", species);
		pagination.addFilterOption("primaryAnnotations.inferredGene.geneSymbol.displayText", gene);
		pagination.addFilterOption("primaryAnnotations.inferredAllele.alleleSymbol.displayText", allele);
		pagination.addFilterOption("phenotypeStatement", phenotype);
		pagination.addFilterOption("primaryAnnotations.dataProvider.abbreviation", dataProvider);
		pagination.addFilterOption("pubmedPubModIDs", reference);
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
		String gene,
		String moleculeType,
		String interactorGene,
		String interactorSpecies,
		String interactorMoleculeType,
		String detectionMethod,
		String source,
		String asc
	) {
		long startTime = System.currentTimeMillis();
		List<String> xrefs = splitCommas(crossReferences);
		requireCrossReferences(xrefs);
		Pagination pagination = new Pagination(page, limit, sortBy, asc);
		pagination.addFilterOption("geneMolecularInteraction.geneAssociationSubject.taxon.species.fullName.keyword", species);
		pagination.addFilterOption("geneMolecularInteraction.geneAssociationSubject.geneSymbol.displayText", gene);
		pagination.addFilterOption("geneMolecularInteraction.interactorAType.name.keyword", moleculeType);
		pagination.addFilterOption("geneMolecularInteraction.geneGeneAssociationObject.geneSymbol.displayText", interactorGene);
		pagination.addFilterOption("geneMolecularInteraction.geneGeneAssociationObject.taxon.species.fullName.keyword", interactorSpecies);
		pagination.addFilterOption("geneMolecularInteraction.interactorBType.name.keyword", interactorMoleculeType);
		pagination.addFilterOption("geneMolecularInteraction.detectionMethod.name.keyword", detectionMethod);
		pagination.addFilterOption("geneMolecularInteraction.interactionIdORgeneMolecularInteraction.aggregationDatabase.nameORgeneMolecularInteraction.interactionSource.nameORgeneMolecularInteraction.crossReferences.displayName", source);
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
		String gene,
		String geneRole,
		String geneticPerturbation,
		String interactorGene,
		String interactorSpecies,
		String interactorRole,
		String interactorGeneticPerturbation,
		String interactionType,
		String phenotypes,
		String source,
		String asc
	) {
		long startTime = System.currentTimeMillis();
		List<String> xrefs = splitCommas(crossReferences);
		requireCrossReferences(xrefs);
		Pagination pagination = new Pagination(page, limit, sortBy, asc);
		pagination.addFilterOption("geneGeneticInteraction.geneAssociationSubject.taxon.species.fullName.keyword", species);
		pagination.addFilterOption("geneGeneticInteraction.geneAssociationSubject.geneSymbol.displayText", gene);
		pagination.addFilterOption("geneGeneticInteraction.interactorARole.name.keyword", geneRole);
		pagination.addFilterOption("geneGeneticInteraction.interactorAGeneticPerturbation.alleleSymbol.displayText", geneticPerturbation);
		pagination.addFilterOption("geneGeneticInteraction.geneGeneAssociationObject.geneSymbol.displayText", interactorGene);
		pagination.addFilterOption("geneGeneticInteraction.geneGeneAssociationObject.taxon.species.fullName.keyword", interactorSpecies);
		pagination.addFilterOption("geneGeneticInteraction.interactorBRole.name.keyword", interactorRole);
		pagination.addFilterOption("geneGeneticInteraction.interactorBGeneticPerturbation.alleleSymbol.displayText", interactorGeneticPerturbation);
		pagination.addFilterOption("geneGeneticInteraction.interactionType.name.keyword", interactionType);
		pagination.addFilterOption("geneGeneticInteraction.phenotypesOrTraits", phenotypes);
		pagination.addFilterOption("geneGeneticInteraction.interactionIdORgeneGeneticInteraction.crossReferences.displayName", source);
		validate(pagination);
		try {
			return timed(referenceDataESService.getGeneticInteractions(xrefs, pagination), startTime);
		} catch (Exception e) {
			throw restError("Error while retrieving genetic interactions by reference", e);
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

	@Override
	public JsonResultResponse<LiteratureSummaryDocument> getLatestLiteratureSummariesByMod(String q, Integer latest) {
		long startTime = System.currentTimeMillis();
		try {
			return timed(referenceDataESService.getLatestLiteratureSummaryByMod(q, latest), startTime);
		} catch (Exception e) {
			throw restError("Error while retrieving latest literature summaries by MOD", e);
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
