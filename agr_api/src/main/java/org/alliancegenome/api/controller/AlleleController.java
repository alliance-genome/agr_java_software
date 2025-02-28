package org.alliancegenome.api.controller;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.alliancegenome.api.entity.AlleleDiseaseAnnotationDocument;
import org.alliancegenome.api.entity.AllelePhenotypeAnnotationDocument;
import org.alliancegenome.api.rest.interfaces.AlleleRESTInterface;
import org.alliancegenome.api.service.*;
import org.alliancegenome.api.service.helper.APIServiceHelper;
import org.alliancegenome.api.translators.tdf.DiseaseAnnotationToTdfTranslator;
import org.alliancegenome.cache.repository.helper.JsonResultResponse;
import org.alliancegenome.core.exceptions.RestErrorException;
import org.alliancegenome.core.exceptions.RestErrorMessage;
import org.alliancegenome.core.translators.tdf.AlleleToTdfTranslator;
import org.alliancegenome.core.translators.tdf.PhenotypeAnnotationToTdfTranslator;
import org.alliancegenome.es.model.query.FieldFilter;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.neo4j.entity.node.Allele;
import org.alliancegenome.neo4j.entity.node.Variant;

import java.time.LocalDateTime;

@Slf4j
@RequestScoped
public class AlleleController implements AlleleRESTInterface {

	@Inject
	AlleleService alleleService;

	@Inject
	VariantService variantService;

	@Inject
	DiseaseESService diseaseESService;

	@Inject
	PhenotypeESService phenotypeESService;
	//@Inject
	//private HttpRequest request;

	private AlleleToTdfTranslator translator = new AlleleToTdfTranslator();
	private PhenotypeAnnotationToTdfTranslator phenotypeTranslator = new PhenotypeAnnotationToTdfTranslator();
	private final DiseaseAnnotationToTdfTranslator diseaseToTdfTranslator = new DiseaseAnnotationToTdfTranslator();

	@Override
	public Allele getAllele(String id) {
		return alleleService.getById(id);
	}

	@Override
	public JsonResultResponse<Variant> getVariantsPerAllele(String id,
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
			JsonResultResponse<Variant> alleles = variantService.getVariants(id, pagination);
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
		JsonResultResponse<Variant> response = getVariantsPerAllele(id,
			Integer.MAX_VALUE,
			1,
			sortBy,
			variantType,
			consequence);
		Response.ResponseBuilder responseBuilder = Response.ok(translator.getAllVariantsRows(response.getResults()));
		APIServiceHelper.setDownloadHeader(id, EntityType.ALLELE, EntityType.VARIANT, responseBuilder);
		return responseBuilder.build();
	}

	@Override
	public JsonResultResponse<Allele> getAllelesPerSpecies(String species, Integer limit, Integer page, String sortBy, String asc) {
		long startTime = System.currentTimeMillis();
		Pagination pagination = new Pagination(page, limit, sortBy, asc);
		JsonResultResponse<Allele> response = alleleService.getAllelesBySpecies(species, pagination);
		response.setHttpServletRequest(null);
		Long duration = (System.currentTimeMillis() - startTime) / 1000;
		response.setRequestDuration(duration.toString());
		return response;
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
		APIServiceHelper.setDownloadHeader(id, EntityType.ALLELE, EntityType.PHENOTYPE, responseBuilder);
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
		APIServiceHelper.setDownloadHeader(alleleID, EntityType.ALLELE, EntityType.DISEASE, responseBuilder);
		return responseBuilder.build();
	}

}
