package org.alliancegenome.api.controller;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.alliancegenome.api.dto.ExpressionSummary;
import org.alliancegenome.api.dto.JoinTypeValue;
import org.alliancegenome.api.entity.AlleleVariantSequence;
import org.alliancegenome.api.entity.DiseaseRibbonSummary;
import org.alliancegenome.api.rest.interfaces.GeneRESTInterface;
import org.alliancegenome.api.service.AlleleService;
import org.alliancegenome.api.service.EntityType;
import org.alliancegenome.api.service.ExpressionService;
import org.alliancegenome.api.service.GeneService;
import org.alliancegenome.api.service.helper.APIServiceHelper;
import org.alliancegenome.cache.repository.ExpressionCacheRepository;
import org.alliancegenome.cache.repository.OrthologyCacheRepository;
import org.alliancegenome.cache.repository.helper.JsonResultResponse;
import org.alliancegenome.core.api.service.DiseaseService;
import org.alliancegenome.core.api.service.InteractionColumnFieldMapping;
import org.alliancegenome.core.exceptions.RestErrorException;
import org.alliancegenome.core.exceptions.RestErrorMessage;
import org.alliancegenome.core.translators.tdf.AlleleToTdfTranslator;
import org.alliancegenome.api.translators.tdf.DiseaseAnnotationToTdfTranslator;
import org.alliancegenome.core.translators.tdf.InteractionToTdfTranslator;
import org.alliancegenome.core.translators.tdf.PhenotypeAnnotationToTdfTranslator;
import org.alliancegenome.es.model.query.FieldFilter;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.neo4j.entity.DiseaseAnnotation;
import org.alliancegenome.neo4j.entity.DiseaseSummary;
import org.alliancegenome.neo4j.entity.EntitySummary;
import org.alliancegenome.neo4j.entity.PhenotypeAnnotation;
import org.alliancegenome.neo4j.entity.PrimaryAnnotatedEntity;
import org.alliancegenome.neo4j.entity.node.Allele;
import org.alliancegenome.neo4j.entity.node.Gene;
import org.alliancegenome.neo4j.entity.node.InteractionGeneJoin;
import org.alliancegenome.neo4j.view.OrthologView;
import org.alliancegenome.neo4j.view.OrthologyFilter;
import org.apache.commons.collections.CollectionUtils;

import lombok.extern.log4j.Log4j2;

@Log4j2
@RequestScoped
public class GeneController implements GeneRESTInterface {

	@Inject GeneService geneService;

	@Inject AlleleService alleleService;

	@Inject OrthologyCacheRepository orthologyService;

	@Inject ExpressionCacheRepository expressionCacheRepository;

	@Inject DiseaseService diseaseService;

	@Inject OrthologyCacheRepository orthologyCacheService;

	//@Inject
	//private HttpRequest request;

	@Inject ExpressionService service;

	private static final PhenotypeAnnotationToTdfTranslator translator = new PhenotypeAnnotationToTdfTranslator();
	private static final AlleleToTdfTranslator alleleTanslator = new AlleleToTdfTranslator();
	private static final InteractionToTdfTranslator interactionTanslator = new InteractionToTdfTranslator();
	private static final DiseaseAnnotationToTdfTranslator diseaseTranslator = new DiseaseAnnotationToTdfTranslator();

	@Override
	public Gene getGene(String id) {
		Gene gene = geneService.getById(id);
		if (gene == null) {
			RestErrorMessage error = new RestErrorMessage("No gene found with ID: " + id);
			throw new RestErrorException(error);
		} else {
			return gene;
		}
	}

	@Override
	public JsonResultResponse<Allele> getAllelesPerGene(String id,
														Integer limit,
														Integer page,
														String sortBy,
														String asc,
														String symbol,
														String synonym,
														String variantType,
														String molecularConsequence,
														String hasDisease,
														String hasPhenotype,
														String category) {
		long startTime = System.currentTimeMillis();
		Pagination pagination = new Pagination(page, limit, sortBy, asc);
		pagination.addFieldFilter(FieldFilter.SYMBOL, symbol);
		pagination.addFieldFilter(FieldFilter.SYNONYMS, synonym);
		pagination.addFieldFilter(FieldFilter.ALLELE_CATEGORY, category);
		pagination.addFieldFilter(FieldFilter.VARIANT_TYPE, variantType);
		pagination.addFieldFilter(FieldFilter.HAS_DISEASE, hasDisease);
		pagination.addFieldFilter(FieldFilter.HAS_PHENOTYPE, hasPhenotype);
		pagination.addFieldFilter(FieldFilter.MOLECULAR_CONSEQUENCE, molecularConsequence);
		if (pagination.hasErrors()) {
			RestErrorMessage message = new RestErrorMessage();
			message.setErrors(pagination.getErrors());
			throw new RestErrorException(message);
		}

		try {
			JsonResultResponse<Allele> alleles = geneService.getAlleles(id, pagination);
			alleles.setHttpServletRequest(null);
			alleles.calculateRequestDuration(startTime);
			return alleles;
		} catch (Exception e) {
			String errorMessage = "Error while retrieving allele info";
			log.error(errorMessage, e);
			RestErrorMessage error = new RestErrorMessage();
			if (e.getMessage() != null) {
				errorMessage += "\n" + e.getMessage();
			}
			error.addErrorMessage(errorMessage);
			throw new RestErrorException(error);
		}
	}

	@Override
	public JsonResultResponse<AlleleVariantSequence> getAllelesVariantPerGene(String id,
																			  Integer limit,
																			  Integer page,
																			  String sortBy,
																			  String asc,
																			  String symbol,
																			  String associatedGeneSymbol,
																			  String synonyms,
																			  String hgvsgName,
																			  String variantType,
																			  String molecularConsequence,
																			  String impact,
																			  String sequenceFeatureType,
																			  String sequenceFeature,
																			  String variantPolyphen,
																			  String variantSift,
																			  String hasDisease,
																			  String hasPhenotype,
																			  String category,
																			  String location) {
		long startTime = System.currentTimeMillis();
		Pagination pagination = new Pagination(page, limit, sortBy, asc);
		pagination.addFieldFilter(FieldFilter.SYMBOL, symbol);
		pagination.addFieldFilter(FieldFilter.SYNONYMS, synonyms);
		pagination.addFieldFilter(FieldFilter.ALLELE_CATEGORY, category);
		pagination.addFieldFilter(FieldFilter.VARIANT_TYPE, variantType);
		pagination.addFieldFilter(FieldFilter.HAS_DISEASE, hasDisease);
		pagination.addFieldFilter(FieldFilter.HAS_PHENOTYPE, hasPhenotype);
		pagination.addFieldFilter(FieldFilter.VARIANT_IMPACT, impact);
		pagination.addFieldFilter(FieldFilter.MOLECULAR_CONSEQUENCE, molecularConsequence);
		pagination.addFieldFilter(FieldFilter.VARIANT_POLYPHEN, variantPolyphen);
		pagination.addFieldFilter(FieldFilter.VARIANT_SIFT, variantSift);
		pagination.addFieldFilter(FieldFilter.SEQUENCE_FEATURE_TYPE, sequenceFeatureType);
		pagination.addFieldFilter(FieldFilter.SEQUENCE_FEATURE, sequenceFeature);
		pagination.addFieldFilter(FieldFilter.ASSOCIATED_GENE, associatedGeneSymbol);
		pagination.addFieldFilter(FieldFilter.VARIANT_LOCATION, location);
		if (pagination.hasErrors()) {
			RestErrorMessage message = new RestErrorMessage();
			message.setErrors(pagination.getErrors());
			throw new RestErrorException(message);
		}

		try {
			JsonResultResponse<AlleleVariantSequence> alleles = geneService.getAllelesAndVariantInfo(id, pagination);
			alleles.setHttpServletRequest(null);
			alleles.calculateRequestDuration(startTime);
			return alleles;
		} catch (Exception e) {
			String errorMessage = "Error while retrieving allele info";
			log.error(errorMessage, e);
			RestErrorMessage error = new RestErrorMessage();
			if (e.getMessage() != null) {
				errorMessage += "\n" + e.getMessage();
			}
			error.addErrorMessage(errorMessage);
			throw new RestErrorException(error);
		}
	}

	@Override
	public Response getAllelesVariantPerGeneDownload(String id,
													 String symbol,
													 String associatedGeneSymbol,
													 String synonyms,
													 String hgvsgName,
													 String variantType,
													 String molecularConsequence,
													 String impact,
													 String sequenceFeatureType,
													 String sequenceFeature,
													 String variantPolyphen,
													 String variantSift,
													 String hasDisease,
													 String hasPhenotype,
													 String category,
													 String location) {
		JsonResultResponse<AlleleVariantSequence> alleles = getAllelesVariantPerGene(id,
				Integer.MAX_VALUE,
				1,
				null,
				null,
				symbol,
				associatedGeneSymbol,
				synonyms,
				hgvsgName,
				variantType,
				molecularConsequence,
				impact,
				sequenceFeatureType,
				sequenceFeature,
				variantPolyphen,
				variantSift,
				hasDisease,
				hasPhenotype,
				category,
				location);

		Response.ResponseBuilder responseBuilder = Response.ok(alleleTanslator.getAllAlleleVariantDetailRows(alleles.getResults()));
		APIServiceHelper.setDownloadHeader(id, EntityType.GENE, EntityType.ALLELESANDVARIANT, responseBuilder);
		return responseBuilder.build();
	}

	@Override
	public Response getAllelesPerGeneDownload(String id,
											  String sortBy,
											  String asc,
											  String symbol,
											  String synonym,
											  String variantType,
											  String molecularConsequence,
											  String phenotype,
											  String source,
											  String disease) {
		Pagination pagination = new Pagination(1, Integer.MAX_VALUE, sortBy, asc);
		pagination.addFieldFilter(FieldFilter.SYMBOL, symbol);
		pagination.addFieldFilter(FieldFilter.SYNONYMS, synonym);
		pagination.addFieldFilter(FieldFilter.SOURCE, source);
		pagination.addFieldFilter(FieldFilter.DISEASE, disease);
		pagination.addFieldFilter(FieldFilter.VARIANT_TYPE, variantType);
		pagination.addFieldFilter(FieldFilter.PHENOTYPE, phenotype);
		pagination.addFieldFilter(FieldFilter.MOLECULAR_CONSEQUENCE, molecularConsequence);
		if (pagination.hasErrors()) {
			RestErrorMessage message = new RestErrorMessage();
			message.setErrors(pagination.getErrors());
			throw new RestErrorException(message);
		}

		JsonResultResponse<Allele> alleles = geneService.getAlleles(id, pagination);

		Response.ResponseBuilder responseBuilder = Response.ok(alleleTanslator.getAllRows(alleles.getResults()));
		APIServiceHelper.setDownloadHeader(id, EntityType.GENE, EntityType.ALLELE, responseBuilder);
		return responseBuilder.build();
	}


	@Override
	public JsonResultResponse<InteractionGeneJoin> getInteractions(String id, Integer limit, Integer page, String sortBy, String asc,
																   String moleculeType,
																   JoinTypeValue joinType,
																   String interactorGeneSymbol,
																   String interactorSpecies,
																   String interactorMoleculeType,
																   String detectionMethod,
																   String source,
																   String reference,
																   String role,
																   String geneticPerturbation,
																   String interacotorRole,
																   String interactorGeneticPerturbation,
																   String phenotypes,
																   String interactionType,
																   @Context UriInfo info) {
		long startTime = System.currentTimeMillis();
		Pagination pagination = new Pagination(page, limit, sortBy, asc, new InteractionColumnFieldMapping());
		pagination.addFieldFilter(FieldFilter.MOLECULE_TYPE, moleculeType);
		pagination.addFieldFilter(FieldFilter.JOIN_TYPE, joinType.getName());
		pagination.addFieldFilter(FieldFilter.INTERACTOR_GENE_SYMBOL, interactorGeneSymbol);
		pagination.addFieldFilter(FieldFilter.INTERACTOR_SPECIES, interactorSpecies);
		pagination.addFieldFilter(FieldFilter.INTERACTOR_MOLECULE_TYPE, interactorMoleculeType);
		pagination.addFieldFilter(FieldFilter.DETECTION_METHOD, detectionMethod);
		pagination.addFieldFilter(FieldFilter.SOURCE, source);
		pagination.addFieldFilter(FieldFilter.INTERACTOR_REFERENCE, reference);
		//for genetic interaction
		pagination.addFieldFilter(FieldFilter.ROLE, role);
		pagination.addFieldFilter(FieldFilter.GENETIC_PERTURBATION, geneticPerturbation);
		pagination.addFieldFilter(FieldFilter.INTERACTOR_ROLE, interacotorRole);
		pagination.addFieldFilter(FieldFilter.INTERACTOR_GENETIC_PERTURBATION, interactorGeneticPerturbation);
		pagination.addFieldFilter(FieldFilter.PHENOTYPES, phenotypes);
		pagination.addFieldFilter(FieldFilter.INTERACTION_TYPE, interactionType);
		// Todo: needs to be made generic
		//pagination.validateFilterValues(info.getQueryParameters());
		if (pagination.hasErrors()) {
			RestErrorMessage message = new RestErrorMessage();
			message.setErrors(pagination.getErrors());
			throw new RestErrorException(message);
		}
		try {
			JsonResultResponse<InteractionGeneJoin> interactions = geneService.getInteractions(id, pagination, joinType.getName());
			interactions.setHttpServletRequest(null);
			interactions.calculateRequestDuration(startTime);
			return interactions;
		} catch (Exception e) {
			log.error("Error while retrieving interaction data", e);
			RestErrorMessage error = new RestErrorMessage();
			error.addErrorMessage(e.getMessage());
			throw new RestErrorException(error);
		}
	}

	@Override
	public Response getInteractionsDownload(String id, String sortBy, String asc,
											String moleculeType,
											JoinTypeValue joinType,
											String interactorGeneSymbol,
											String interactorSpecies,
											String interactorMoleculeType,
											String detectionMethod,
											String source,
											String reference,
											String role,
											String geneticPerturbation,
											String interacotorRole,
											String interactorGeneticPerturbation,
											String phenotypes,
											String interactionType
	) {
		Pagination pagination = new Pagination(1, Integer.MAX_VALUE, sortBy, asc);
		pagination.addFieldFilter(FieldFilter.MOLECULE_TYPE, moleculeType);
		pagination.addFieldFilter(FieldFilter.JOIN_TYPE, joinType.getName());
		pagination.addFieldFilter(FieldFilter.INTERACTOR_GENE_SYMBOL, interactorGeneSymbol);
		pagination.addFieldFilter(FieldFilter.INTERACTOR_SPECIES, interactorSpecies);
		pagination.addFieldFilter(FieldFilter.INTERACTOR_MOLECULE_TYPE, interactorMoleculeType);
		pagination.addFieldFilter(FieldFilter.DETECTION_METHOD, detectionMethod);
		pagination.addFieldFilter(FieldFilter.SOURCE, source);
		pagination.addFieldFilter(FieldFilter.FREFERENCE, reference);
		//for genetic interaction
		pagination.addFieldFilter(FieldFilter.ROLE, role);
		pagination.addFieldFilter(FieldFilter.GENETIC_PERTURBATION, geneticPerturbation);
		pagination.addFieldFilter(FieldFilter.INTERACTOR_ROLE, interacotorRole);
		pagination.addFieldFilter(FieldFilter.INTERACTOR_GENETIC_PERTURBATION, interactorGeneticPerturbation);
		pagination.addFieldFilter(FieldFilter.PHENOTYPES, phenotypes);
		pagination.addFieldFilter(FieldFilter.INTERACTION_TYPE, interactionType);
		JsonResultResponse<InteractionGeneJoin> interactions = geneService.getInteractions(id, pagination);

		Response.ResponseBuilder responseBuilder = Response.ok(interactionTanslator.getAllRows(interactions.getResults()));
		APIServiceHelper.setDownloadHeader(id, EntityType.GENE, EntityType.INTERACTION, joinType.getName(), responseBuilder);
		return responseBuilder.build();
	}

	@Override
	public JsonResultResponse<PhenotypeAnnotation> getPhenotypeAnnotations(String id, Integer limit, Integer page, String sortBy,
																		   String geneticEntity,
																		   String geneticEntityType,
																		   String phenotype,
																		   String reference,
																		   String asc) {
		long startTime = System.currentTimeMillis();
		try {
			JsonResultResponse<PhenotypeAnnotation> phenotypes = getPhenotypeAnnotationDocumentJsonResultResponse(id, limit, page, sortBy, geneticEntity, geneticEntityType, phenotype, reference, asc);
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
	public Response getPhenotypeAnnotationsDownloadFile(
			String id,
			String sortBy,
			String geneticEntity,
			String geneticEntityType,
			String phenotype,
			String reference,
			String asc) {
		// retrieve all records
		JsonResultResponse<PhenotypeAnnotation> response =
				getPhenotypeAnnotationDocumentJsonResultResponse(id, Integer.MAX_VALUE, 1, sortBy,
						geneticEntity,
						geneticEntityType,
						phenotype,
						reference,
						asc);
		Response.ResponseBuilder responseBuilder = Response.ok(translator.getAllRows(response.getResults()));
		APIServiceHelper.setDownloadHeader(id, EntityType.GENE, EntityType.PHENOTYPE, responseBuilder);
		return responseBuilder.build();
	}


	@Override
	public JsonResultResponse<DiseaseAnnotation> getDiseaseAnnotations(String id, Integer limit, Integer page, String sortBy,
																	   String geneticEntity,
																	   String geneticEntityType,
																	   String disease,
																	   String reference,
																	   String asc) {
		long startTime = System.currentTimeMillis();
		try {
			JsonResultResponse<DiseaseAnnotation> diseases = getDiseaseAnnotationDocumentJsonResultResponse(id, limit, page, sortBy, geneticEntity, geneticEntityType, disease, reference, asc);
			diseases.setHttpServletRequest(null);
			diseases.calculateRequestDuration(startTime);
			return diseases;
		} catch (Exception e) {
			log.error("Error while retrieving disease", e);
			RestErrorMessage error = new RestErrorMessage();
			error.addErrorMessage(e.getMessage());
			throw new RestErrorException(error);
		}
	}

	@Override
	public Response getDiseaseAnnotationsDownloadFile(
			String id,
			String sortBy,
			String geneticEntity,
			String geneticEntityType,
			String disease,
			String reference,
			String asc) {
		// retrieve all records
		JsonResultResponse<DiseaseAnnotation> response =
				getDiseaseAnnotationDocumentJsonResultResponse(id, Integer.MAX_VALUE, 1, sortBy,
						geneticEntity,
						geneticEntityType,
						disease,
						reference,
						asc);
		Response.ResponseBuilder responseBuilder = Response.ok(diseaseTranslator.getAllRowsForGenes(response.getResults()));
		APIServiceHelper.setDownloadHeader(id, EntityType.GENE, EntityType.DISEASE, responseBuilder);
		return responseBuilder.build();
	}


	@Override
	public JsonResultResponse<PrimaryAnnotatedEntity> getPrimaryAnnotatedEntityForModel(String id,
																						Integer limit,
																						Integer page,
																						String sortBy,
																						String modelName,
																						String species,
																						String disease,
																						String phenotype,
																						String source,
																						String asc) {
		long startTime = System.currentTimeMillis();
		Pagination pagination = new Pagination(page, limit, sortBy, asc);
		pagination.addFieldFilter(FieldFilter.SPECIES, species);
		pagination.addFieldFilter(FieldFilter.DISEASE, disease);
		pagination.addFieldFilter(FieldFilter.PHENOTYPE, phenotype);
		pagination.addFieldFilter(FieldFilter.SOURCE, source);
		pagination.addFieldFilter(FieldFilter.MODEL_NAME, modelName);
		if (pagination.hasErrors()) {
			RestErrorMessage message = new RestErrorMessage();
			message.setErrors(pagination.getErrors());
			throw new RestErrorException(message);
		}
		try {
			JsonResultResponse<PrimaryAnnotatedEntity> response = diseaseService.getDiseaseAnnotationsWithGeneAndAGM(id, pagination);
			response.setHttpServletRequest(null);
			response.calculateRequestDuration(startTime);
			return response;
		} catch (Exception e) {
			log.error("Error while retrieving disease annotations by allele", e);
			RestErrorMessage error = new RestErrorMessage();
			error.addErrorMessage(e.getMessage());
			throw new RestErrorException(error);
		}
	}

	private JsonResultResponse<PhenotypeAnnotation> getPhenotypeAnnotationDocumentJsonResultResponse(String id, Integer limit, Integer page, String sortBy, String geneticEntity, String geneticEntityType, String phenotype, String reference, String asc) {
		if (sortBy.isEmpty())
			sortBy = FieldFilter.PHENOTYPE.getName();
		Pagination pagination = new Pagination(page, limit, sortBy, asc);
		pagination.addFieldFilter(FieldFilter.GENETIC_ENTITY, geneticEntity);
		pagination.addFieldFilter(FieldFilter.GENETIC_ENTITY_TYPE, geneticEntityType);
		pagination.addFieldFilter(FieldFilter.PHENOTYPE, phenotype);
		pagination.addFieldFilter(FieldFilter.FREFERENCE, reference);
		JsonResultResponse<PhenotypeAnnotation> phenotypeAnnotations = geneService.getPhenotypeAnnotations(id, pagination);
		phenotypeAnnotations.addAnnotationSummarySupplementalData(getPhenotypeSummary(id));
		return phenotypeAnnotations;
	}

	private JsonResultResponse<DiseaseAnnotation> getDiseaseAnnotationDocumentJsonResultResponse(String id, Integer limit, Integer page, String sortBy, String geneticEntity, String geneticEntityType, String disease, String reference, String asc) {
		if (sortBy.isEmpty())
			sortBy = FieldFilter.DISEASE.getName();
		Pagination pagination = new Pagination(page, limit, sortBy, asc);
		pagination.addFieldFilter(FieldFilter.GENETIC_ENTITY, geneticEntity);
		pagination.addFieldFilter(FieldFilter.GENETIC_ENTITY_TYPE, geneticEntityType);
		pagination.addFieldFilter(FieldFilter.DISEASE, disease);
		pagination.addFieldFilter(FieldFilter.FREFERENCE, reference);
		JsonResultResponse<DiseaseAnnotation> diseaseAnnotations = diseaseService.getDiseaseAnnotations(id, pagination);

		return diseaseAnnotations;
	}

	private JsonResultResponse<DiseaseAnnotation> getEmpiricalDiseaseAnnotation(String id,
																				Integer limit,
																				Integer page,
																				String sortBy,
																				String geneticEntity,
																				String geneticEntityType,
																				String disease,
																				String associationType,
																				String evidenceCode,
																				String source,
																				String reference,
																				String asc,
																				UriInfo ui) {
		Pagination pagination = new Pagination(page, limit, sortBy, asc);
		pagination.addFieldFilter(FieldFilter.GENETIC_ENTITY, geneticEntity);
		pagination.addFieldFilter(FieldFilter.GENETIC_ENTITY_TYPE, geneticEntityType);
		pagination.addFieldFilter(FieldFilter.ASSOCIATION_TYPE, associationType);
		pagination.addFieldFilter(FieldFilter.EVIDENCE_CODE, evidenceCode);
		pagination.addFieldFilter(FieldFilter.SOURCE, source);
		pagination.addFieldFilter(FieldFilter.DISEASE, disease);
		pagination.addFieldFilter(FieldFilter.FREFERENCE, reference);
		MultivaluedMap<String, String> parameterMap = ui.getQueryParameters();
		List<String> invalidFilterNames = parameterMap.entrySet().stream()
				.filter(entry -> FieldFilter.hasFieldFilterPrefix(entry.getKey()) && !FieldFilter.isFieldFilterValue(entry.getKey()))
				.map(Map.Entry::getKey)
				.collect(Collectors.toList());
		pagination.setInvalidFilterList(invalidFilterNames);
		return diseaseService.getDiseaseAnnotations(id, pagination);
	}

	@Override
	public JsonResultResponse<OrthologView> getGeneOrthology(String id,
															 List<String> geneIDs,
															 String geneLister,
															 String stringencyFilter,
															 String taxonID,
															 String method,
															 Integer limit,
															 Integer page) {

		List<String> geneList = new ArrayList<>();
		if (id != null) {
			geneList.add(id);
		}
		if (geneLister != null) {
			List<String> ids = Arrays.asList(geneLister.split(","));
			geneList.addAll(ids);
		}
		if (CollectionUtils.isNotEmpty(geneIDs)) {
			geneList.addAll(geneIDs);
		}
		Pagination pagination = new Pagination(page, limit, null, null);
		pagination.addFieldFilter(FieldFilter.STRINGENCY, stringencyFilter);
		pagination.addFieldFilter(FieldFilter.ORTHOLOGY_METHOD, method);
		pagination.addFieldFilter(FieldFilter.ORTHOLOGY_TAXON, taxonID);
		final JsonResultResponse<OrthologView> response = orthologyService.getOrthologyMultiGeneJson(geneList, pagination);
		response.setHttpServletRequest(null);
		return response;
	}

	@Override
	public JsonResultResponse<OrthologView> getGeneOrthologyWithExpression(String id,
																		   String stringencyFilter) {

		long startTime = System.currentTimeMillis();
		List<String> geneList = new ArrayList<>();
		if (id != null) {
			geneList.add(id);
		}

		OrthologyFilter orthologyFilter = new OrthologyFilter(stringencyFilter, null, null);
		orthologyFilter.setStart(1);
		JsonResultResponse<OrthologView> orthologs = orthologyCacheService.getOrthologyGenes(geneList, orthologyFilter);
		List<OrthologView> filteredList = orthologs.getResults().stream()
				.filter(orthologView -> expressionCacheRepository.hasExpression(orthologView.getHomologGene().getPrimaryKey()))
				.sorted(Comparator.comparing(orthologView -> orthologView.getHomologGene().getSymbol().toLowerCase()))
				.collect(Collectors.toList());
		orthologs.setResults(filteredList);
		orthologs.setTotal(filteredList.size());
		orthologs.setHttpServletRequest(null);
		orthologs.calculateRequestDuration(startTime);
		return orthologs;
	}

	@Override
	public ExpressionSummary getExpressionSummary(String id) {
		return service.getExpressionSummary(id);
	}

	@Override
	// the List passed in here is unmodifiable
	public DiseaseRibbonSummary getDiseaseRibbonSummary(String id,
														List<String> geneIDs,
														String includeNegation) {
		List<String> ids = new ArrayList<>();
		if (geneIDs != null)
			ids.addAll(geneIDs);
		if (!id.equals("*"))
			ids.add(id);

		try {
			return diseaseService.getDiseaseRibbonSummary(ids, includeNegation);
		} catch (Exception e) {
			log.error("Error while creating disease ribbon summary", e);
			RestErrorMessage error = new RestErrorMessage();
			error.addErrorMessage(e.getMessage());
			throw new RestErrorException(error);
		}
	}

	@Override
	public EntitySummary getInteractionSummary(String geneID) {
		return geneService.getInteractionSummary(geneID);
	}

	@Override
	public JsonResultResponse<DiseaseAnnotation> getDiseaseByExperiment(String id,
																		Integer limit,
																		Integer page,
																		String sortBy,
																		String geneticEntity,
																		String geneticEntityType,
																		String disease,
																		String associationType,
																		String evidenceCode,
																		String source,
																		String reference,
																		String asc,
																		UriInfo ui) {
		return getEmpiricalDiseaseAnnotation(id,
				limit,
				page,
				sortBy,
				geneticEntity,
				geneticEntityType,
				disease,
				associationType,
				evidenceCode,
				source,
				reference,
				asc,
				ui);
	}

	@Override
	public Response getDiseaseByExperimentDownload(String id,
												   String sortBy,
												   String geneticEntity,
												   String geneticEntityType,
												   String disease,
												   String associationType,
												   String evidenceCode,
												   String source,
												   String reference,
												   String asc,
												   UriInfo ui) {
		JsonResultResponse<DiseaseAnnotation> response = getEmpiricalDiseaseAnnotation(id,
				Integer.MAX_VALUE,
				null,
				sortBy,
				geneticEntity,
				geneticEntityType,
				disease,
				associationType,
				evidenceCode,
				source,
				reference,
				asc,
				ui);
		Response.ResponseBuilder responseBuilder = Response.ok(diseaseTranslator.getEmpiricalDiseaseByGene(response.getResults()));
		responseBuilder.type(MediaType.TEXT_PLAIN_TYPE);
		responseBuilder.header("Content-Disposition", "attachment; filename=\"DiseaseAssociationsViaEmpiricalData-" + id.replace(":", "-") + ".tsv\"");
		return responseBuilder.build();
	}

	@Override
	public DiseaseSummary getDiseaseSummary(String id, String type) {
		DiseaseSummary.Type diseaseType = DiseaseSummary.Type.getType(type);
		return diseaseService.getDiseaseSummary(id, diseaseType);
	}

	@Override
	public EntitySummary getPhenotypeSummary(String id) {
		return geneService.getPhenotypeSummary(id);
	}

	@Override
	public JsonResultResponse<Allele> getTransgenicAlleles(String geneID,
														   Integer limit,
														   Integer page,
														   String sortBy,
														   String alleleSymbol,
														   String constructSymbol,
														   String constructRegulatedGene,
														   String constructTargetedGene,
														   String constructExpressedGene,
														   String species,
														   String hasPhenotype,
														   String hasDisease,
														   UriInfo ui) {
		if (sortBy != null && sortBy.isBlank())
			sortBy = "transgenicAllele";
		Pagination pagination = new Pagination(page, limit, sortBy, null);
		pagination.addFieldFilter(FieldFilter.SYMBOL, alleleSymbol);
		pagination.addFieldFilter(FieldFilter.SPECIES, species);
		pagination.addFieldFilter(FieldFilter.TRANSGENE_HAS_PHENOTYPE, hasPhenotype);
		pagination.addFieldFilter(FieldFilter.TRANSGENE_HAS_DISEASE, hasDisease);
		pagination.addFieldFilter(FieldFilter.CONSTRUCT_SYMBOL, constructSymbol);
		pagination.addFieldFilter(FieldFilter.CONSTRUCT_TARGETED_GENE, constructTargetedGene);
		pagination.addFieldFilter(FieldFilter.CONSTRUCT_REGULATED_GENE, constructRegulatedGene);
		pagination.addFieldFilter(FieldFilter.CONSTRUCT_EXPRESSED_GENE, constructExpressedGene);
		if (pagination.hasErrors()) {
			RestErrorMessage message = new RestErrorMessage();
			message.setErrors(pagination.getErrors());
			throw new RestErrorException(message);
		}
		try {
			JsonResultResponse<Allele> response = alleleService.getTransgenicAlleles(geneID, pagination);
			response.setHttpServletRequest(null);
			return response;
		} catch (Exception e) {
			log.error("Error while retrieving transgenic allele info", e);
			RestErrorMessage error = new RestErrorMessage();
			error.addErrorMessage(e.getMessage());
			throw new RestErrorException(error);
		}
	}


	@Override
	public Response getTransgenicAllelesPerGeneDownload(String geneId,
														String sortBy,
														String alleleSymbol,
														String constructSymbol,
														String constructRegulatedGene,
														String constructTargetedGene,
														String constructExpressedGene,
														String species,
														String hasPhenotype,
														String hasDisease,
														UriInfo ui) {
		JsonResultResponse<Allele> alleles = getTransgenicAlleles(geneId,
				Integer.MAX_VALUE,
				1,
				sortBy,
				alleleSymbol,
				constructSymbol,
				constructRegulatedGene,
				constructTargetedGene,
				constructExpressedGene,
				species,
				hasPhenotype,
				hasDisease,
				ui);

		Response.ResponseBuilder responseBuilder = Response.ok(alleleTanslator.getAllTransgenicAlleleRows(alleles.getResults()));
		APIServiceHelper.setDownloadHeader(geneId, EntityType.GENE, EntityType.TRANSGENICALLELE, responseBuilder);
		return responseBuilder.build();
	}

}
