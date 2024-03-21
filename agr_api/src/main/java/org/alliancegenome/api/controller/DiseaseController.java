package org.alliancegenome.api.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.alliancegenome.api.entity.AlleleDiseaseAnnotationDocument;
import org.alliancegenome.api.entity.DiseaseAnnotationDocument;
import org.alliancegenome.api.entity.GeneDiseaseAnnotationDocument;
import org.alliancegenome.api.rest.interfaces.DiseaseRESTInterface;
import org.alliancegenome.api.service.DiseaseESService;
import org.alliancegenome.api.service.EntityType;
import org.alliancegenome.api.service.helper.APIServiceHelper;
import org.alliancegenome.api.translators.tdf.DiseaseAnnotationToTdfTranslator;
import org.alliancegenome.cache.repository.helper.JsonResultResponse;
import org.alliancegenome.cache.repository.helper.SortingField;
import org.alliancegenome.core.api.service.DiseaseService;
import org.alliancegenome.core.exceptions.RestErrorException;
import org.alliancegenome.core.exceptions.RestErrorMessage;
import org.alliancegenome.core.util.FileHelper;
import org.alliancegenome.es.model.query.FieldFilter;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.neo4j.entity.DiseaseAnnotation;
import org.alliancegenome.neo4j.entity.SpeciesType;
import org.alliancegenome.neo4j.entity.node.DOTerm;
import org.alliancegenome.neo4j.view.View;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static org.alliancegenome.api.service.EntityType.DISEASE;
import static org.alliancegenome.api.service.EntityType.GENE;

@RequestScoped
public class DiseaseController implements DiseaseRESTInterface {

	// @Inject
	// private HttpRequest request;

	@Inject
	ObjectMapper mapper;

	@Inject
	DiseaseService diseaseService;

	@Inject
	DiseaseESService diseaseESService;

	private final DiseaseAnnotationToTdfTranslator translator = new DiseaseAnnotationToTdfTranslator();

	@Override
	public DOTerm getDisease(String id) {
		DOTerm doTerm = diseaseService.getById(id);
		if (doTerm == null) {
			RestErrorMessage error = new RestErrorMessage("No disease term found with ID: " + id);
			throw new RestErrorException(error);
		} else {
			return doTerm;
		}
	}

	@Override
	public JsonResultResponse<DiseaseAnnotation> getDiseaseAnnotationsSorted(String id, Integer limit, Integer page, String sortBy, String geneName, String species, String geneticEntity, String geneticEntityType, String disease, String source, String reference, String evidenceCode, String basedOnGeneSymbol, String associationType, String asc) {
		long startTime = System.currentTimeMillis();
		Pagination pagination = new Pagination(page, limit, sortBy, asc);
		pagination.addFieldFilter(FieldFilter.GENE_NAME, geneName);
		pagination.addFieldFilter(FieldFilter.SPECIES, species);
		pagination.addFieldFilter(FieldFilter.GENETIC_ENTITY, geneticEntity);
		pagination.addFieldFilter(FieldFilter.GENETIC_ENTITY_TYPE, geneticEntityType);
		pagination.addFieldFilter(FieldFilter.DISEASE, disease);
		pagination.addFieldFilter(FieldFilter.SOURCE, source);
		pagination.addFieldFilter(FieldFilter.FREFERENCE, reference);
		pagination.addFieldFilter(FieldFilter.EVIDENCE_CODE, evidenceCode);
		pagination.addFieldFilter(FieldFilter.BASED_ON_GENE, basedOnGeneSymbol);
		pagination.addFieldFilter(FieldFilter.ASSOCIATION_TYPE, associationType);
		if (pagination.hasErrors()) {
			RestErrorMessage message = new RestErrorMessage();
			message.setErrors(pagination.getErrors());
			throw new RestErrorException(message);
		}
		try {
			JsonResultResponse<DiseaseAnnotation> response = diseaseService.getDiseaseAnnotationsByDisease(id, pagination);
			response.setHttpServletRequest(null);
			response.calculateRequestDuration(startTime);

			return response;
		} catch (Exception e) {
			Log.error("Error while retrieving disease annotations", e);
			RestErrorMessage error = new RestErrorMessage();
			error.addErrorMessage(e.getMessage());
			throw new RestErrorException(error);
		}
	}

	@Override
	public JsonResultResponse<AlleleDiseaseAnnotationDocument> getDiseaseAnnotationsByAllele(String id, Integer limit, Integer page, String sortBy, String geneName, String alleleName, String species, String disease, String source, String reference, String evidenceCode, String associationType, String asc) {
		long startTime = System.currentTimeMillis();
		// The @DefaultValue only kicks in if the value is null.
		// need to handle an empty value manually here.
		if (sortBy.trim().isEmpty())
			sortBy = SortingField.DISEASE_ALLELE_DEFAULT.toString();
		Pagination pagination = new Pagination(page, limit, sortBy, asc);
		pagination.addFieldFilter(FieldFilter.GENE_NAME, geneName);
		pagination.addFieldFilter(FieldFilter.ALLELE, alleleName);
		pagination.addFieldFilter(FieldFilter.SPECIES, species);
		pagination.addFieldFilter(FieldFilter.DISEASE, disease);
		pagination.addFieldFilter(FieldFilter.SOURCE, source);
		pagination.addFieldFilter(FieldFilter.FREFERENCE, reference);
		pagination.addFieldFilter(FieldFilter.EVIDENCE_CODE, evidenceCode);
		pagination.addFieldFilter(FieldFilter.ASSOCIATION_TYPE, associationType);
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
	public Response getDiseaseAnnotationsByAlleleDownload(String id, String sortBy, String geneName, String alleleName, String species, String disease, String source, String reference, String evidenceCode, String associationType, String asc) {
		JsonResultResponse<AlleleDiseaseAnnotationDocument> response = getDiseaseAnnotationsByAllele(id, Integer.MAX_VALUE, null, sortBy, geneName, alleleName, species, disease, source, reference, evidenceCode, associationType, asc);
//		Response.ResponseBuilder responseBuilder = Response.ok(translator.getAllRowsForAllele(response.getResults());

//		APIServiceHelper.setDownloadHeader(id, EntityType.DISEASE, EntityType.ALLELE, responseBuilder);
//		return responseBuilder.build();
		return Response.ok().build();
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
		JsonResultResponse<GeneDiseaseAnnotationDocument> response = getDiseaseAnnotationsByGene(id, 150000, page, sortBy, geneName, geneID, species, diseaseName, source, reference, evidenceCode, basedOnGeneSymbol, associationType,diseaseQualifier, asc);
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
					taxonIDs = SpeciesType.getAllTaxonIDs();
				}
				data = data.replace("${taxonIDs}", taxonIDs);
				data += allRowsForGenes;
				responseBuilder = Response.ok(data);
				APIServiceHelper.setDownloadHeader(id, EntityType.DISEASE, EntityType.GENE, responseBuilder);
			} else if (downloadFileType.equalsIgnoreCase("JSON")) {
				try {
					String data = mapper.writerWithView(View.DiseaseAnnotationSummary.class).writeValueAsString(response);
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
		pagination.addFilterOption("subject.taxon.name", species);
		pagination.addFilterOption("evidenceCodes.abbreviation", evidenceCode);
		pagination.addFilterOption("generatedRelationString.keyword", associationType);
		pagination.addFilterOption("diseaseQualifiers.keyword", diseaseQualifier);
		pagination.addFilterOption("pubmedPubModIDs", reference);
		pagination.addFilterOption("primaryAnnotations.dataProvider.sourceOrganization.abbreviation", source);
		pagination.addFilterOption("primaryAnnotations.with.geneSymbol.displayText", basedOnGeneSymbol);
		pagination.addFilterOption("object.name", diseaseName);
		if (StringUtils.isNotEmpty(geneID)) {
			pagination.addFilterOption("subject.curie", geneID);
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
			Log.error("Error while retrieving disease annotations by allele", e);
			RestErrorMessage error = new RestErrorMessage();
			error.addErrorMessage(e.getMessage());
			throw new RestErrorException(error);
		}
	}

	@Override
	public JsonResultResponse<DiseaseAnnotation> getDiseaseAnnotationsForModel(String id, Integer limit, Integer page, String sortBy, String modelName, String geneName, String species, String disease, String source, String reference, String evidenceCode, String associationType, String asc) {
		long startTime = System.currentTimeMillis();
		Pagination pagination = new Pagination(page, limit, sortBy, asc);
		pagination.addFieldFilter(FieldFilter.GENE_NAME, geneName);
		pagination.addFieldFilter(FieldFilter.SPECIES, species);
		pagination.addFieldFilter(FieldFilter.DISEASE, disease);
		pagination.addFieldFilter(FieldFilter.SOURCE, source);
		pagination.addFieldFilter(FieldFilter.FREFERENCE, reference);
		pagination.addFieldFilter(FieldFilter.EVIDENCE_CODE, evidenceCode);
		pagination.addFieldFilter(FieldFilter.MODEL_NAME, modelName);
		pagination.addFieldFilter(FieldFilter.ASSOCIATION_TYPE, associationType);
		if (pagination.hasErrors()) {
			RestErrorMessage message = new RestErrorMessage();
			message.setErrors(pagination.getErrors());
			throw new RestErrorException(message);
		}
		try {
			JsonResultResponse<DiseaseAnnotation> response = diseaseService.getDiseaseAnnotationsWithAGM(id, pagination);
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
	public Response getDiseaseAnnotationsForModelDownload(String id, String sortBy, String modelName, String geneName, String species, String disease, String source, String reference, String evidenceCode, String associationType, String asc) {
		JsonResultResponse<DiseaseAnnotation> response = getDiseaseAnnotationsForModel(id, Integer.MAX_VALUE, null, sortBy, modelName, geneName, species, disease, source, reference, evidenceCode, associationType, asc);
		Response.ResponseBuilder responseBuilder = Response.ok(translator.getAllRowsForModel(response.getResults()));
		APIServiceHelper.setDownloadHeader(id, EntityType.DISEASE, EntityType.MODEL, responseBuilder);
		return responseBuilder.build();
	}

	@Override
	public Response getDiseaseAnnotationsDownloadFile(String id, String sortBy, String geneName, String species, String geneticEntity, String geneticEntityType, String disease, String source, String reference, String evidenceCode, String basedOnGeneSymbol, String associationType, String asc) {
		Pagination pagination = new Pagination(1, Integer.MAX_VALUE, sortBy, asc);
		pagination.addFieldFilter(FieldFilter.GENE_NAME, geneName);
		pagination.addFieldFilter(FieldFilter.SPECIES, species);
		pagination.addFieldFilter(FieldFilter.GENETIC_ENTITY, geneticEntity);
		pagination.addFieldFilter(FieldFilter.GENETIC_ENTITY_TYPE, geneticEntityType);
		pagination.addFieldFilter(FieldFilter.DISEASE, disease);
		pagination.addFieldFilter(FieldFilter.SOURCE, source);
		pagination.addFieldFilter(FieldFilter.FREFERENCE, reference);
		pagination.addFieldFilter(FieldFilter.EVIDENCE_CODE, evidenceCode);
		pagination.addFieldFilter(FieldFilter.BASED_ON_GENE, basedOnGeneSymbol);
		pagination.addFieldFilter(FieldFilter.ASSOCIATION_TYPE, associationType);
		Response.ResponseBuilder responseBuilder = null;
		if (pagination.hasErrors()) {
			RestErrorMessage message = new RestErrorMessage();
			message.setErrors(pagination.getErrors());
			throw new RestErrorException(message);
		}
		try {
			JsonResultResponse<DiseaseAnnotation> jsonResponse = diseaseService.getDiseaseAnnotationsByDisease(id, pagination);
			responseBuilder = Response.ok(translator.getAllRowsForGenes(jsonResponse.getResults()));
			responseBuilder.type(MediaType.TEXT_PLAIN_TYPE);
			APIServiceHelper.setDownloadHeader(id, DISEASE, GENE, responseBuilder);
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
	public String getDiseaseAnnotationsDownload(String id) {
		Pagination pagination = new Pagination(1, Integer.MAX_VALUE, null, null);
		// retrieve all records
		return translator.getAllRowsForGenes(diseaseService.getDiseaseAnnotationsByDisease(id, pagination).getResults());
	}

	@Override
	public JsonResultResponse<GeneDiseaseAnnotationDocument> getDiseaseAnnotationsRibbonDetails(String focusTaxonId,
																								List<String> geneIDs,
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
																								String asc) {

		LocalDateTime startDate = LocalDateTime.now();
		Pagination pagination = new Pagination(page, limit, sortBy, asc);
		pagination.addFilterOptions(filterOptions);
		pagination.addFilterOption("object.name", diseaseTerm);
		pagination.addFilterOption("evidenceCodes.abbreviation", evidenceCode);
		pagination.addFilterOption("generatedRelationString.keyword", associationType);
		pagination.addFilterOption("diseaseQualifiers.keyword", diseaseQualifier);
		pagination.addFilterOption("pubmedPubModIDs", filterReference);
		pagination.addFilterOption("subject.geneSymbol.displayText", filterGene);
		pagination.addFilterOption("primaryAnnotations.with.geneSymbol.displayText", basedOnGeneSymbol);
		pagination.addFilterOption("primaryAnnotations.dataProvider.sourceOrganization.abbreviation OR primaryAnnotations.secondaryDataProvider.sourceOrganization.abbreviation", filterSource);

		// TODO: remove when SC data is fixed:
		String SC = "Saccharomyces cerevisiae";
		String SC_288C = "Saccharomyces cerevisiae S288C";
		if (filterSpecies != null) {
			if (filterSpecies.equals(SC)) {
				pagination.addFilterOption("subject.taxon.name.keyword", SC_288C);
			} else {
				pagination.addFilterOption("subject.taxon.name.keyword", filterSpecies);
			}
		}

		if (pagination.hasErrors()) {
			RestErrorMessage message = new RestErrorMessage();
			message.setErrors(pagination.getErrors());
			throw new RestErrorException(message);
		}
		try {
			JsonResultResponse<GeneDiseaseAnnotationDocument> response = diseaseESService.getRibbonDiseaseAnnotations(focusTaxonId, geneIDs, termID, pagination, !includeNegation, debug);
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
	public Response getDiseaseAnnotationsRibbonDetailsDownload(String focusTaxonId, List<String> geneIDs, String termID, String filterSpecies, String filterGene, String filterReference, String diseaseTerm, String filterSource, String geneticEntity, String geneticEntityType, String associationType, String diseaseQualifier, String evidenceCode, String basedOnGeneSymbol, Boolean includeNegation, Boolean debug, String sortBy, String asc) {

		LocalDateTime startDate = LocalDateTime.now();
		Response.ResponseBuilder responseBuilder;
		try {
			JsonResultResponse<GeneDiseaseAnnotationDocument> response = getDiseaseAnnotationsRibbonDetails(focusTaxonId, geneIDs, termID, null, filterSpecies, filterGene, filterReference, diseaseTerm, filterSource, geneticEntity, geneticEntityType, associationType, diseaseQualifier, evidenceCode, basedOnGeneSymbol, includeNegation, debug, 150000, 1, sortBy, asc);
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
	public Response getDiseaseAnnotationsBySpeciesDownload(List<String> speciesIDs, String diseaseID, String sortBy) {

		if (CollectionUtils.isEmpty(speciesIDs)) {
			speciesIDs = Arrays.stream(SpeciesType.values()).map(SpeciesType::getTaxonID).collect(Collectors.toList());
		} else {
			speciesIDs = speciesIDs.stream().map(SpeciesType::getTaxonId).collect(Collectors.toList());
		}
		List<DiseaseAnnotation> alleleAnnotations = new ArrayList<>();
		List<DiseaseAnnotation> geneAnnotations = new ArrayList<>();
		List<DiseaseAnnotation> modelAnnotations = new ArrayList<>();
		speciesIDs.forEach(species -> {
//			alleleAnnotations.addAll(getDiseaseAnnotationsByAllele(diseaseID, Integer.MAX_VALUE, null, sortBy, null, null, species, null, null, null, null, null, null).getResults());
			modelAnnotations.addAll(getDiseaseAnnotationsForModel(diseaseID, Integer.MAX_VALUE, null, sortBy, null, null, species, null, null, null, null, null, null).getResults());
			//geneAnnotations.addAll(getDiseaseAnnotationsByGene(diseaseID, Integer.MAX_VALUE, null, sortBy, null, species, null, null, null, null, null, null, null).getResults());
		});
		Response.ResponseBuilder responseBuilder = Response.ok(translator.getAllRowsForGenesAndAlleles(geneAnnotations, alleleAnnotations, modelAnnotations));
		return responseBuilder.build();
	}

}
