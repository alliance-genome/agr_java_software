package org.alliancegenome.api.controller;

import java.time.LocalDateTime;

import org.alliancegenome.api.entity.AlleleDiseaseAnnotationDocument;
import org.alliancegenome.api.entity.AllelePhenotypeAnnotationDocument;
import org.alliancegenome.api.rest.interfaces.AlleleRESTInterface;
import org.alliancegenome.api.service.AlleleESService;

import org.alliancegenome.api.service.DiseaseESService;
import org.alliancegenome.api.service.EntityType;
import org.alliancegenome.api.service.PhenotypeESService;
import org.alliancegenome.api.service.helper.APIServiceHelper;
import org.alliancegenome.api.translators.tdf.DiseaseAnnotationToTdfTranslator;
import org.alliancegenome.api.translators.tdf.PhenotypeAnnotationToTdfTranslator;
import org.alliancegenome.cache.repository.helper.JsonResultResponse;
import org.alliancegenome.core.exceptions.RestErrorException;
import org.alliancegenome.core.exceptions.RestErrorMessage;
import org.alliancegenome.core.translators.tdf.AlleleToTdfTranslator;
import org.alliancegenome.curation_api.model.document.es.AlleleSummaryDocument;
import org.alliancegenome.api.entity.TransgenicAlleleSummaryDocument;
import org.alliancegenome.curation_api.model.document.es.VariantSummaryDocument;
import org.alliancegenome.es.model.query.FieldFilter;
import org.alliancegenome.es.model.query.Pagination;
import org.apache.commons.collections4.CollectionUtils;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequestScoped
public class AlleleController implements AlleleRESTInterface {

	@Inject
	AlleleESService alleleEsService;

	@Inject
	DiseaseESService diseaseESService;

	@Inject
	AlleleESService alleleESService;

	@Inject
	PhenotypeESService phenotypeESService;
	//@Inject
	//private HttpRequest request;

	private AlleleToTdfTranslator translator = new AlleleToTdfTranslator();
	private PhenotypeAnnotationToTdfTranslator phenotypeTranslator = new PhenotypeAnnotationToTdfTranslator();
	private final DiseaseAnnotationToTdfTranslator diseaseToTdfTranslator = new DiseaseAnnotationToTdfTranslator();

	@Override
	public AlleleSummaryDocument getAllele(String id) {
		AlleleSummaryDocument alleleSummary = alleleESService.getById(id);
		if (alleleSummary == null) {
			RestErrorMessage error = new RestErrorMessage("No allele found with ID: " + id);
			throw new RestErrorException(error);
		} else {
			return alleleSummary;
		}
	}

	@Override
	public TransgenicAlleleSummaryDocument getAlleleConstructs(String alleleId) {
		JsonResultResponse<TransgenicAlleleSummaryDocument> transgenicAlleles = alleleEsService.getTransgenicAlleles(alleleId);
		if (transgenicAlleles == null) {
			return null;
		}
		if (CollectionUtils.isEmpty(transgenicAlleles.getResults())) {
			return null;
		}
		return transgenicAlleles.getResults().get(0);
	}


	@Override
	public JsonResultResponse<VariantSummaryDocument> getVariantsPerAllele(String id,
																		Integer limit,
																		Integer page,
																		String sortBy,
																		String variantType,
																		String molecularConsequence) {
		long startTime = System.currentTimeMillis();
		Pagination pagination = new Pagination(page, limit, sortBy, null);
		pagination.addFieldFilter(FieldFilter.VARIANT_TYPE, variantType);
		pagination.addFieldFilter(FieldFilter.MOLECULAR_CONSEQUENCE, molecularConsequence);
		if (pagination.hasErrors()) {
			RestErrorMessage message = new RestErrorMessage();
			message.setErrors(pagination.getErrors());
			throw new RestErrorException(message);
		}

		try {
			JsonResultResponse<VariantSummaryDocument> alleles = alleleEsService.getVariantSummary(id, pagination);
			alleles.setHttpServletRequest(null);
			alleles.calculateRequestDuration(startTime);
			return alleles;
		} catch (Exception e) {
			log.error("Error while retrieving variant info", e);
			RestErrorMessage error = new RestErrorMessage();
			error.addErrorMessage(e.getMessage());
			throw new RestErrorException(error);
		}
	}

	@Override
	public Response getVariantsPerAlleleDownload(String id, String sortBy, String variantType, String consequence) {
		JsonResultResponse<VariantSummaryDocument> response = getVariantsPerAllele(id, 100000, 1, sortBy, variantType, consequence);
		Response.ResponseBuilder responseBuilder = Response.ok(translator.getAllVariantsRows(response.getResults()));
		APIServiceHelper.setDownloadHeader(id, EntityType.ALLELE, EntityType.VARIANT, responseBuilder);
		return responseBuilder.build();
	}

	@Override
	public JsonResultResponse<AllelePhenotypeAnnotationDocument> getPhenotypePerAllele(
		String id,
		Integer limit,
		Integer page,
		String phenotype,
		String source,
		String reference,
		String sortBy) {
		long startTime = System.currentTimeMillis();
		Pagination pagination = new Pagination(page, limit, sortBy, null);
		pagination.addFilterOption("phenotypeStatement", phenotype);
		pagination.addFilterOption("pubmedPubModIDs", reference);
		pagination.addFilterOption("primaryAnnotations.dataProvider.abbreviation", source);
		try {
			JsonResultResponse<AllelePhenotypeAnnotationDocument> phenotypes = phenotypeESService.getAllelePhenotypeAnnotations(id, pagination, false);
			phenotypes.setHttpServletRequest(null);
			phenotypes.calculateRequestDuration(startTime);
			return phenotypes;
		} catch (Exception e) {
			log.error("Error while retrieving phenotypes", e);
			RestErrorMessage error = new RestErrorMessage();
			error.addErrorMessage(e.getMessage());
			throw new RestErrorException(error);
		}
	}

	@Override
	public Response getPhenotypesPerAlleleDownload(String id, String phenotype, String source, String reference, String sortBy) {
		// retrieve all records
		JsonResultResponse<AllelePhenotypeAnnotationDocument> response =
			getPhenotypePerAllele(id,
				250000,
				1,
				phenotype,
				source,
				reference,
				sortBy);
		Response.ResponseBuilder responseBuilder = Response.ok(phenotypeTranslator.getAllRows(response.getResults()));
		String alleleSymbol = getAllele(id).getAllele().getAlleleSymbol().getFormatText();
		APIServiceHelper.setDownloadHeaderByName(id, alleleSymbol, EntityType.ALLELE, EntityType.PHENOTYPE, responseBuilder);
		return responseBuilder.build();
	}

	@Override
	public JsonResultResponse<AlleleDiseaseAnnotationDocument> getDiseasePerAllele(
		String alleleID,
		String filterOptions,
		String filterReference,
		String diseaseTerm,
		String filterSource,
		String geneticEntity,
		String geneticEntityType,
		String associationType,
		String diseaseQualifier,
		String evidenceCode,
		Boolean debug,
		Integer limit,
		Integer page,
		String sortBy,
		String asc) {

		LocalDateTime startDate = LocalDateTime.now();
		Pagination pagination = new Pagination(page, limit, sortBy, asc);
		pagination.addFilterOptions(filterOptions);
		pagination.addFilterOption("object.name", diseaseTerm);
		pagination.addFilterOption("evidenceCodes.abbreviation", evidenceCode);
		pagination.addFilterOption("generatedRelationString.keyword", associationType);
		pagination.addFilterOption("diseaseQualifiers.keyword", diseaseQualifier);
		pagination.addFilterOption("pubmedPubModIDs", filterReference);
		pagination.addFilterOption("primaryAnnotations.dataProvider.sourceOrganization.abbreviation", filterSource);


		if (pagination.hasErrors()) {
			RestErrorMessage message = new RestErrorMessage();
			message.setErrors(pagination.getErrors());
			throw new RestErrorException(message);
		}
		try {
			JsonResultResponse<AlleleDiseaseAnnotationDocument> response = diseaseESService.getDiseaseAnnotations(alleleID, pagination, false, debug);
			response.setHttpServletRequest(null);
			response.calculateRequestDuration(startDate);
			return response;
		} catch (Exception e) {
			log.error("Error while retrieving disease annotations", e);
			RestErrorMessage error = new RestErrorMessage();
			error.addErrorMessage(e.getMessage());
			throw new RestErrorException(error);
		}
	}


	@Override
	public Response getDiseasePerAlleleDownload(String alleleID,
												String filterOptions,
												String filterReference,
												String diseaseTerm,
												String filterSource,
												String geneticEntity,
												String geneticEntityType,
												String associationType,
												String diseaseQualifier,
												String evidenceCode,
												Boolean debug,
												Integer limit,
												Integer page,
												String sortBy,
												String asc) {
		JsonResultResponse<AlleleDiseaseAnnotationDocument> response = getDiseasePerAllele(alleleID,
			filterOptions,
			filterReference,
			diseaseTerm,
			filterSource,
			geneticEntity,
			geneticEntityType,
			associationType,
			diseaseQualifier,
			evidenceCode,
			debug,
			150000,
			page,
			sortBy,
			asc);
		Response.ResponseBuilder responseBuilder = Response.ok(diseaseToTdfTranslator.getAllRowsForAlleleDiseaseAnnotations(response.getResults()));
		String alleleSymbol = getAllele(alleleID).getAllele().getAlleleSymbol().getFormatText();
		APIServiceHelper.setDownloadHeaderByName(alleleID, alleleSymbol, EntityType.ALLELE, EntityType.DISEASE, responseBuilder);
		return responseBuilder.build();
	}

}
