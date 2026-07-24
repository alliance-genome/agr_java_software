package org.alliancegenome.api.controller;

import static org.alliancegenome.api.service.EntityType.DISEASE;
import static org.alliancegenome.api.service.EntityType.GENE;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

import org.alliancegenome.core.document.AGMDiseaseAnnotationDocument;
import org.alliancegenome.core.document.AlleleDiseaseAnnotationDocument;
import org.alliancegenome.core.document.GeneDiseaseAnnotationDocument;
import org.alliancegenome.curation_api.model.entities.ontology.DOTerm;
import org.alliancegenome.api.rest.interfaces.DiseaseRESTInterface;
import org.alliancegenome.api.service.DiseaseESService;
import org.alliancegenome.api.service.EntityType;
import org.alliancegenome.api.service.helper.APIServiceHelper;
import org.alliancegenome.api.translators.tdf.DiseaseAnnotationToTdfTranslator;
import org.alliancegenome.api.response.JsonResultResponse;
import org.alliancegenome.api.exceptions.RestErrorException;
import org.alliancegenome.api.exceptions.RestErrorMessage;
import org.alliancegenome.core.util.FileHelper;
import org.alliancegenome.curation_api.model.document.es.DiseaseSummaryDocument;
import org.alliancegenome.api.es.dao.SpeciesESDAO;
import org.alliancegenome.api.es.query.Pagination;
import org.alliancegenome.core.view.PublicView;
import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@RequestScoped
public class DiseaseController implements DiseaseRESTInterface {

	@Inject
	ObjectMapper mapper;

	@Inject
	DiseaseESService diseaseESService;

	@Inject
	SpeciesESDAO speciesESDAO;

	private final DiseaseAnnotationToTdfTranslator translator = new DiseaseAnnotationToTdfTranslator();

	@Override
	public DiseaseSummaryDocument getDisease(String id) {
		DiseaseSummaryDocument diseaseSummary = diseaseESService.getById(id);
		if (diseaseSummary == null) {
			RestErrorMessage error = new RestErrorMessage("No disease term found with ID: " + id);
			throw new RestErrorException(error);
		} else {
			return diseaseSummary;
		}
	}

	@Override
	public JsonResultResponse<AlleleDiseaseAnnotationDocument> getDiseaseAnnotationsByAllele(String id,
																							Integer limit,
																							Integer page,
																							String sortBy,
																							String geneName,
																							String alleleName,
																							String diseaseName,
																							String species,
																							String disease,
																							String dataProvider,
																							String reference,
																							String evidenceCode,
																							String associationType,
																							String diseaseQualifier,
																							String asc) {
		long startTime = System.currentTimeMillis();
		// The @DefaultValue only kicks in if the value is null.
		// need to handle an empty value manually here.
		if (sortBy.trim().isEmpty()) {
			sortBy = "DISEASE_ALLELE_DEFAULT";
		}
		Pagination pagination = new Pagination(page, limit, sortBy, asc);
		pagination.addFilterOption("subject.taxon.species.fullName.keyword", species);
		pagination.addFilterOption("subject.alleleSymbol.displayText", alleleName);
		pagination.addFilterOption("evidenceCodes.abbreviation", evidenceCode);
		pagination.addFilterOption("generatedRelationString.keyword", associationType);
		pagination.addFilterOption("diseaseQualifiers.keyword", diseaseQualifier);
		pagination.addFilterOption("pubmedPublications.referencedCurie", reference);
		pagination.addFilterOption("primaryAnnotations.dataProvider.abbreviation", dataProvider);
		pagination.addFilterOption("object.name", diseaseName);
		if (pagination.hasErrors()) {
			RestErrorMessage message = new RestErrorMessage();
			message.setErrors(pagination.getErrors());
			throw new RestErrorException(message);
		}
		try {
			JsonResultResponse<AlleleDiseaseAnnotationDocument> response = diseaseESService.getDiseaseAnnotationsWithAlleles(id, pagination);
			response.setHttpServletRequest(null);
			response.calculateRequestDuration(startTime);
			return response;
		} catch (Exception e) {
			Log.error("Error while retrieving disease annotations by allele", e);
			RestErrorMessage error = new RestErrorMessage();
			error.addErrorMessage(e.getMessage());
			throw new RestErrorException(error);
		}
	}

	@Override
	public Response getDiseaseAnnotationsByAlleleDownload(
		String id,
		Integer limit,
		Integer page,
		String sortBy,
		String geneName,
		String alleleName,
		String diseaseName,
		String species,
		String disease,
		String source,
		String reference,
		String evidenceCode,
		String associationType,
		String diseaseQualifier,
		boolean fullDownload,
		String downloadFileType,
		String asc) {
		
		JsonResultResponse<AlleleDiseaseAnnotationDocument> response = getDiseaseAnnotationsByAllele(id, 150000, null, sortBy, geneName, alleleName, disease, species, disease, source, reference, evidenceCode, associationType, diseaseQualifier, asc);
		Response.ResponseBuilder responseBuilder = null;
		String allRowsForAlleles = translator.getAllRowsForAlleleDiseaseAnnotations(response.getResults());

		if (fullDownload) {
			if (downloadFileType == null || downloadFileType.equalsIgnoreCase("tsv")) {
				String data = FileHelper.getFileContent("templates/all-disease-association-file-header.txt");
				SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm");
				String dateString = format.format(new Date());
				data = data.replace("${date}", dateString);

				String taxonIDs = species;
				if (StringUtils.isEmpty(taxonIDs)) {
					taxonIDs = speciesESDAO.getAllTaxonIDs();
				}
				data = data.replace("${taxonIDs}", taxonIDs);
				data += allRowsForAlleles;
				responseBuilder = Response.ok(data);
				APIServiceHelper.setDownloadHeader(id, EntityType.DISEASE, EntityType.ALLELE, responseBuilder);
			} else if (downloadFileType.equalsIgnoreCase("JSON")) {
				try {
					String data = mapper.writerWithView(PublicView.DiseaseAnnotationSummary.class).writeValueAsString(response);
					responseBuilder = Response.ok(data);
					APIServiceHelper.setDownloadHeader(id, EntityType.DISEASE, EntityType.ALLELE, responseBuilder);
				} catch (JsonProcessingException e) {
					e.printStackTrace();
				}
			} else {
				responseBuilder = Response.ok("The file type [" + downloadFileType + "] is not supported. Please use tsv or JSON");
				APIServiceHelper.setDownloadHeader(id, EntityType.DISEASE, EntityType.ALLELE, responseBuilder);
			}
		} else {
			responseBuilder = Response.ok(allRowsForAlleles);
			APIServiceHelper.setDownloadHeader(id, EntityType.DISEASE, EntityType.ALLELE, responseBuilder);
		}
		return responseBuilder.build();
	}

	@Override
	public Response getDiseaseAnnotationsByGeneDownload(String id,
														Integer limit,
														Integer page,
														String sortBy,
														String geneName,
														String geneID,
														String species,
														String diseaseName,
														String source,
														String reference,
														String evidenceCode,
														String basedOnGeneSymbol,
														String associationType,
														String diseaseQualifier,
														boolean fullDownload,
														String downloadFileType,
														String asc) {
		JsonResultResponse<GeneDiseaseAnnotationDocument> response = getDiseaseAnnotationsByGene(id, 250000, null, sortBy, geneName, geneID, species, diseaseName, source, reference, evidenceCode, basedOnGeneSymbol, associationType, diseaseQualifier, asc);
		Response.ResponseBuilder responseBuilder = null;
		String allRowsForGenes = translator.getAllRowsForAssociatedGenes(response.getResults());
		if (fullDownload) {
			if (downloadFileType == null || downloadFileType.equalsIgnoreCase("tsv")) {
				String data = FileHelper.getFileContent("templates/all-disease-association-file-header.txt");
				SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm");
				String dateString = format.format(new Date());
				data = data.replace("${date}", dateString);

				String taxonIDs = species;
				if (StringUtils.isEmpty(taxonIDs)) {
					taxonIDs = speciesESDAO.getAllTaxonIDs();
				}
				data = data.replace("${taxonIDs}", taxonIDs);
				data += allRowsForGenes;
				responseBuilder = Response.ok(data);
				APIServiceHelper.setDownloadHeader(id, EntityType.DISEASE, EntityType.GENE, responseBuilder);
			} else if (downloadFileType.equalsIgnoreCase("JSON")) {
				try {
					String data = mapper.writerWithView(PublicView.DiseaseAnnotationSummary.class).writeValueAsString(response);
					responseBuilder = Response.ok(data);
					APIServiceHelper.setDownloadHeader(id, EntityType.DISEASE, EntityType.GENE, responseBuilder);
				} catch (JsonProcessingException e) {
					e.printStackTrace();
				}
			} else {
				responseBuilder = Response.ok("The file type [" + downloadFileType + "] is not supported. Please use tsv or JSON");
				APIServiceHelper.setDownloadHeader(id, EntityType.DISEASE, EntityType.GENE, responseBuilder);
			}
		} else {
			responseBuilder = Response.ok(allRowsForGenes);
			APIServiceHelper.setDownloadHeader(id, EntityType.DISEASE, EntityType.GENE, responseBuilder);
		}
		return responseBuilder.build();

	}

	@Override
	public JsonResultResponse<GeneDiseaseAnnotationDocument> getDiseaseAnnotationsByGene(String diseaseID,
																						Integer limit,
																						Integer page,
																						String sortBy,
																						String geneName,
																						String geneID,
																						String species,
																						String diseaseName,
																						String source,
																						String reference,
																						String evidenceCode,
																						String basedOnGeneSymbol,
																						String associationType,
																						String diseaseQualifier,
																						String asc) {
		long startTime = System.currentTimeMillis();
		Pagination pagination = new Pagination(page, limit, sortBy, asc);
		pagination.addFilterOption("subject.geneSymbol.displayText", geneName);
		pagination.addFilterOption("evidenceCodes.abbreviation", evidenceCode);
		pagination.addFilterOption("generatedRelationString.keyword", associationType);
		pagination.addFilterOption("diseaseQualifiers.keyword", diseaseQualifier);
		pagination.addFilterOption("pubmedPublications.referencedCurie", reference);
		pagination.addFilterOption("primaryAnnotations.dataProvider.abbreviation", source);
		pagination.addFilterOption("primaryAnnotations.with.geneSymbol.displayText", basedOnGeneSymbol);
		pagination.addFilterOption("object.name", diseaseName);
		if (StringUtils.isNotEmpty(geneID)) {
			pagination.addFilterOption("subject.curie", geneID);
		}
		if (species != null) {
			pagination.addFilterOption("subject.taxon.species.fullName.keyword", species);
		}


		if (pagination.hasErrors()) {
			RestErrorMessage message = new RestErrorMessage();
			message.setErrors(pagination.getErrors());
			throw new RestErrorException(message);
		}
		try {
			JsonResultResponse<GeneDiseaseAnnotationDocument> response = diseaseESService.getDiseaseAnnotationsWithGenes(diseaseID, pagination, true, false);
			response.setHttpServletRequest(null);
			response.calculateRequestDuration(startTime);

			return response;
		} catch (Exception e) {
			Log.error("Error while retrieving disease annotations by gene", e);
			RestErrorMessage error = new RestErrorMessage();
			error.addErrorMessage(e.getMessage());
			throw new RestErrorException(error);
		}
	}

	@Override
	public JsonResultResponse<AGMDiseaseAnnotationDocument> getDiseaseAnnotationsForModel(String diseaseID,
																						Integer limit,
																						Integer page,
																						String sortBy,
																						String modelName,
																						String geneName,
																						String species,
																						String disease,
																						String source,
																						String reference,
																						String evidenceCode,
																						String associationType,
																						String diseaseQualifier,
																						String conditionModifier,
																						String experimentalCondition,
																						String geneticModifier,
																						String asc) {
		long startTime = System.currentTimeMillis();
		Pagination pagination = new Pagination(page, limit, sortBy, asc);
		pagination.addFilterOption("subject.agmFullName.displayText", modelName);
		pagination.addFilterOption("subject.taxon.species.fullName.keyword", species);
		pagination.addFilterOption("evidenceCodes.abbreviation", evidenceCode);
		pagination.addFilterOption("generatedRelationString.keyword", associationType);
		pagination.addFilterOption("conditionModifierAggregated", conditionModifier);
		pagination.addFilterOption("experimentalConditionsAggregated", experimentalCondition);
		pagination.addFilterOption("geneticModifierAggregated", geneticModifier);
		pagination.addFilterOption("diseaseQualifiers.keyword", diseaseQualifier);
		pagination.addFilterOption("pubmedPublications.referencedCurie", reference);
		pagination.addFilterOption("primaryAnnotations.dataProvider.abbreviation", source);
		pagination.addFilterOption("object.name", disease);

		if (pagination.hasErrors()) {
			RestErrorMessage message = new RestErrorMessage();
			message.setErrors(pagination.getErrors());
			throw new RestErrorException(message);
		}
		try {
			JsonResultResponse<AGMDiseaseAnnotationDocument> response = diseaseESService.getDiseaseAnnotationsWithModels(diseaseID, pagination, true, false);
			response.setHttpServletRequest(null);
			response.calculateRequestDuration(startTime);

			return response;
		} catch (Exception e) {
			Log.error("Error while retrieving disease annotations by model", e);
			RestErrorMessage error = new RestErrorMessage();
			error.addErrorMessage(e.getMessage());
			throw new RestErrorException(error);
		}
	}

	@Override
	public Response getDiseaseAnnotationsForModelDownload(String id,
														String sortBy,
														String modelName,
														String geneName,
														String species,
														String disease,
														String source,
														String reference,
														String evidenceCode,
														String associationType,
														String diseaseQualifier,
														String conditionModifier,
														String experimentalCondition,
														String geneticModifier,
														String asc) {
		JsonResultResponse<AGMDiseaseAnnotationDocument> response = getDiseaseAnnotationsForModel(id, 200000, null, sortBy, modelName, geneName, species, disease, source, reference, evidenceCode, associationType, diseaseQualifier, conditionModifier, experimentalCondition, geneticModifier, asc);
		Response.ResponseBuilder responseBuilder = Response.ok(translator.getAllRowsForModel(response.getResults()));
		APIServiceHelper.setDownloadHeader(id, EntityType.DISEASE, EntityType.MODEL, responseBuilder);
		return responseBuilder.build();
	}

	@Override
	public JsonResultResponse<GeneDiseaseAnnotationDocument> getDiseaseAnnotationsRibbonDetails(
																								String focusTaxonId,
																								String termID,
																								String filterOptions,
																								String filterSpecies,
																								String filterGene,
																								String filterReference,
																								String diseaseTerm,
																								String filterSource,
																								String geneticEntity,
																								String geneticEntityType,
																								String associationType,
																								String diseaseQualifier,
																								String evidenceCode,
																								String basedOnGeneSymbol,
																								Boolean includeNegation,
																								Boolean debug,
																								Integer limit,
																								Integer page,
																								String sortBy,
																								String asc,
																								List<String> geneIDs) {
		return getDiseaseAnnotationsRibbonDetails(focusTaxonId, termID, filterOptions, filterSpecies, filterGene, filterReference, diseaseTerm, filterSource, geneticEntity, geneticEntityType, associationType, diseaseQualifier, evidenceCode, basedOnGeneSymbol, includeNegation, debug, limit, page, sortBy, asc, geneIDs, false);
	}

	private JsonResultResponse<GeneDiseaseAnnotationDocument> getDiseaseAnnotationsRibbonDetails(
																								String focusTaxonId,
																								String termID,
																								String filterOptions,
																								String filterSpecies,
																								String filterGene,
																								String filterReference,
																								String diseaseTerm,
																								String filterSource,
																								String geneticEntity,
																								String geneticEntityType,
																								String associationType,
																								String diseaseQualifier,
																								String evidenceCode,
																								String basedOnGeneSymbol,
																								Boolean includeNegation,
																								Boolean debug,
																								Integer limit,
																								Integer page,
																								String sortBy,
																								String asc,
																								List<String> geneIDs,
																								boolean includePrimaryAnnotations) {

		LocalDateTime startDate = LocalDateTime.now();
		Pagination pagination = new Pagination(page, limit, sortBy, asc);
		pagination.addFilterOptions(filterOptions);
		pagination.addFilterOption("object.name", diseaseTerm);
		pagination.addFilterOption("evidenceCodes.abbreviation", evidenceCode);
		pagination.addFilterOption("generatedRelationString.keyword", associationType);
		pagination.addFilterOption("diseaseQualifiers.keyword", diseaseQualifier);
		pagination.addFilterOption("pubmedPublications.referencedCurie", filterReference);
		pagination.addFilterOption("subject.geneSymbol.displayText", filterGene);
		pagination.addFilterOption("primaryAnnotations.with.geneSymbol.displayText", basedOnGeneSymbol);
		pagination.addFilterOption("primaryAnnotations.dataProvider.abbreviation OR primaryAnnotations.secondaryDataProvider.abbreviation", filterSource);

		pagination.addFilterOption("subject.taxon.species.fullName.keyword", filterSpecies);

		if (pagination.hasErrors()) {
			RestErrorMessage message = new RestErrorMessage();
			message.setErrors(pagination.getErrors());
			throw new RestErrorException(message);
		}
		try {
			JsonResultResponse<GeneDiseaseAnnotationDocument> response;
			response = diseaseESService.getRibbonDiseaseAnnotations(focusTaxonId, geneIDs, termID, pagination, !includeNegation, debug, includePrimaryAnnotations);
			response.setHttpServletRequest(null);
			response.calculateRequestDuration(startDate);
			return response;
		} catch (Exception e) {
			Log.error("Error while retrieving disease annotations", e);
			RestErrorMessage error = new RestErrorMessage();
			error.addErrorMessage(e.getMessage());
			throw new RestErrorException(error);
		}
	}

	@Override
	public Response getDiseaseAnnotationsRibbonDetailsDownload(
		String focusTaxonId, String termID,
		String filterSpecies, String filterGene, String filterReference,
		String diseaseTerm, String filterSource, String geneticEntity,
		String geneticEntityType, String associationType, String diseaseQualifier,
		String evidenceCode, String basedOnGeneSymbol, Boolean includeNegation,
		Boolean debug, String sortBy, String asc, List<String> geneIDs) {

		LocalDateTime startDate = LocalDateTime.now();
		Response.ResponseBuilder responseBuilder;
		try {
			JsonResultResponse<GeneDiseaseAnnotationDocument> response = getDiseaseAnnotationsRibbonDetails(focusTaxonId, termID, null, filterSpecies, filterGene, filterReference, diseaseTerm, filterSource, geneticEntity, geneticEntityType, associationType, diseaseQualifier, evidenceCode, basedOnGeneSymbol, includeNegation, debug, 150000, 1, sortBy, asc, geneIDs, true);
			response.setHttpServletRequest(null);
			response.calculateRequestDuration(startDate);
			// translate all records
			responseBuilder = Response.ok(translator.getAllRowsForGeneDiseaseAnnotations(response.getResults()));
			responseBuilder.type(MediaType.TEXT_PLAIN_TYPE);
			APIServiceHelper.setDownloadHeader(geneIDs.get(0), GENE, DISEASE, responseBuilder);
		} catch (Exception e) {
			e.printStackTrace();
			Log.error("Error: " + e);
			RestErrorMessage error = new RestErrorMessage();
			error.addErrorMessage(e.getMessage());
			throw new RestErrorException(error);
		}

		return responseBuilder.build();
	}

	@Override
	public JsonResultResponse<org.alliancegenome.curation_api.model.entities.DiseaseAnnotation> getDiseasePrimaryAnnotations(String id, Integer limit, Integer page) {
		LocalDateTime startDate = LocalDateTime.now();
		Pagination pagination = new Pagination(page, limit, null, null);

		if (pagination.hasErrors()) {
			RestErrorMessage message = new RestErrorMessage();
			message.setErrors(pagination.getErrors());
			throw new RestErrorException(message);
		}

		try {
			JsonResultResponse<org.alliancegenome.curation_api.model.entities.DiseaseAnnotation> response =
				diseaseESService.getDiseasePrimaryAnnotations(id, pagination, "gene_disease_annotation");

			response.setHttpServletRequest(null);
			response.calculateRequestDuration(startDate);
			return response;
		} catch (Exception e) {
			Log.error("Error while retrieving disease annotations", e);
			RestErrorMessage error = new RestErrorMessage();
			error.addErrorMessage(e.getMessage());
			throw new RestErrorException(error);
		}
	}

	@Override
	public Long getCountsOfDiseaseAnnotationsByAllele(String diseaseID) {
		String associationType = diseaseESService.getAT("allele_disease_annotation", diseaseID);
		String sortBy = "diseaseAlleleDefault";
		
		JsonResultResponse<AlleleDiseaseAnnotationDocument> response = getDiseaseAnnotationsByAllele(diseaseID, null, null, sortBy, null, null, null, null, null, null, null, null, associationType, null, null);
		return response.getTotal();
	}

	@Override
	public Long getCountsOfDiseaseAnnotationsForModel(String diseaseID) {
		String associationType = diseaseESService.getAT("agm_disease_annotation", diseaseID);
		
		JsonResultResponse<AGMDiseaseAnnotationDocument> response = getDiseaseAnnotationsForModel(diseaseID, null, null, null, null, null, null, null, null, null, null, associationType, null, null, null, null, null);
		return response.getTotal();
	}

	@Override
	public List<DOTerm> getDiseaseAncestors(String diseaseID) {
		return diseaseESService.getAncestors(diseaseID);
	}


	private static final int MAX_BATCH_IDS = 500;

	@Override
	public java.util.Map<String, Object> getBatchDiseaseTerms(String ids) {
		if (ids == null || ids.isBlank()) {
			return java.util.Collections.emptyMap();
		}
		java.util.List<String> idList = java.util.Arrays.stream(ids.split(","))
			.map(String::trim)
			.filter(s -> !s.isEmpty())
			.distinct()
			.toList();
		if (idList.size() > MAX_BATCH_IDS) {
			throw new BadRequestException("ids exceeds maximum of " + MAX_BATCH_IDS);
		}
		return diseaseESService.getBatchTerms(idList);
	}

	@Override
	public java.util.Map<String, java.util.Map<String, Long>> getBatchDiseaseCounts(String ids) {
		if (ids == null || ids.isBlank()) {
			return java.util.Collections.emptyMap();
		}
		java.util.List<String> idList = java.util.Arrays.stream(ids.split(","))
			.map(String::trim)
			.filter(s -> !s.isEmpty())
			.distinct()
			.toList();
		if (idList.size() > MAX_BATCH_IDS) {
			throw new BadRequestException("ids exceeds maximum of " + MAX_BATCH_IDS);
		}
		return diseaseESService.getBatchCounts(idList);
	}

	@Override
	public Long getCountsOfDiseaseAnnotationsByGene(String diseaseID) {
		// Count distinct genes (gene-species pairs) with a positive association,
		// not annotation documents (which over-count genes that roll up to multiple sub-terms,
		// disease qualifiers, or based-on-gene groups).
		return diseaseESService.countDistinctPositiveGenes(diseaseID);
	}

}
