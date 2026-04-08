package org.alliancegenome.api.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.alliancegenome.api.entity.DiseaseRibbonSummary;
import org.alliancegenome.api.entity.GeneGeneticInteractionDocument;
import org.alliancegenome.api.entity.GeneMolecularInteractionDocument;
import org.alliancegenome.api.entity.GenePhenotypeAnnotationDocument;
import org.alliancegenome.api.entity.GeneToGeneOrthologyDocument;
import org.alliancegenome.api.entity.GeneToGeneParalogyDocument;
import org.alliancegenome.api.entity.GeneTransgenicAlleleSummaryDocument;
import org.alliancegenome.api.rest.interfaces.GeneRESTInterface;
import org.alliancegenome.api.service.AGMAnnotationESService;
import org.alliancegenome.api.service.AlleleESService;
import org.alliancegenome.api.service.DiseaseESService;
import org.alliancegenome.api.service.EntityType;
import org.alliancegenome.api.service.GeneESService;
import org.alliancegenome.api.service.GeneService;
import org.alliancegenome.api.service.GeneToGeneParalogyESService;
import org.alliancegenome.api.service.OrthologyESService;
import org.alliancegenome.api.service.PhenotypeESService;
import org.alliancegenome.api.service.TransgenicAlleleESService;
import org.alliancegenome.api.service.helper.APIServiceHelper;
import org.alliancegenome.api.translators.tdf.DiseaseAnnotationToTdfTranslator;
import org.alliancegenome.api.translators.tdf.PhenotypeAnnotationToTdfTranslator;
import org.alliancegenome.cache.repository.helper.JsonResultResponse;
import org.alliancegenome.core.api.service.DiseaseService;
import org.alliancegenome.core.api.service.InteractionColumnFieldMapping;
import org.alliancegenome.core.exceptions.RestErrorException;
import org.alliancegenome.core.exceptions.RestErrorMessage;
import org.alliancegenome.core.translators.tdf.AlleleToTdfTranslator;
import org.alliancegenome.core.translators.tdf.GeneGeneticInteractionToTdfTranslator;
import org.alliancegenome.core.translators.tdf.GeneMolecularInteractionToTdfTranslator;
import org.alliancegenome.curation_api.model.document.es.AGMAnnotationDocument;
import org.alliancegenome.curation_api.model.document.es.ESDocument;
import org.alliancegenome.curation_api.model.document.es.GeneSummaryDocument;
import org.alliancegenome.curation_api.model.document.es.SequenceSummaryDocument;
import org.alliancegenome.es.model.query.FieldFilter;
import org.alliancegenome.es.model.query.Pagination;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequestScoped
public class GeneController implements GeneRESTInterface {

	@Inject
	GeneService geneService;

	@Inject
	GeneESService geneESService;

	@Inject
	AlleleESService alleleESService;

	@Inject
	OrthologyESService orthologyESService;

	@Inject
	DiseaseService diseaseService;

	@Inject
	DiseaseESService diseaseESService;

	@Inject
	GeneToGeneParalogyESService geneToGeneParalogyESService;
	@Inject
	PhenotypeESService phenotypeESService;
	@Inject
	AGMAnnotationESService agmESService;

	@Inject
	TransgenicAlleleESService transgenicAlleleESService;

	private static final PhenotypeAnnotationToTdfTranslator translator = new PhenotypeAnnotationToTdfTranslator();
	private static final AlleleToTdfTranslator alleleTranslator = new AlleleToTdfTranslator();
	private static final GeneGeneticInteractionToTdfTranslator geneticInteractionTranslator = new GeneGeneticInteractionToTdfTranslator();
	private static final GeneMolecularInteractionToTdfTranslator molecularInteractionTranslator = new GeneMolecularInteractionToTdfTranslator();
	private static final DiseaseAnnotationToTdfTranslator diseaseTranslator = new DiseaseAnnotationToTdfTranslator();

	@Override
	public GeneSummaryDocument getGene(String id) {
		GeneSummaryDocument gene = geneESService.getById(id);
		if (gene == null) {
			RestErrorMessage error = new RestErrorMessage("No gene found with ID: " + id);
			throw new RestErrorException(error);
		} else {
			return gene;
		}
	}

	@Override
	public JsonResultResponse<ESDocument> getAllelesPerGene(String id, Integer limit, Integer page, String sortBy, String asc, String symbol, String synonym, String variant, String variantType, String molecularConsequence, String hasDisease, String hasPhenotype, String category) {

		long startTime = System.currentTimeMillis();
		Pagination pagination = new Pagination(page, limit, sortBy, asc);
		pagination.addFilterOption("symbol", symbol);
		pagination.addFilterOption("allele.alleleSynonyms.displayText", synonym);
		pagination.addFilterOption("variantList.curatedVariantGenomicLocations.hgvs", variant);
		pagination.addFilterOption("alterationType.keyword", category);
		pagination.addFilterOption("variantList.variantType.name.keyword", variantType);
		pagination.addFilterOption("hasDisease", hasDisease);
		pagination.addFilterOption("hasPhenotype", hasPhenotype);
		pagination.addFilterOption("variantList.curatedVariantGenomicLocations.predictedVariantConsequences.vepConsequences.name.keyword", molecularConsequence);

		if (pagination.hasErrors()) {
			RestErrorMessage message = new RestErrorMessage();
			message.setErrors(pagination.getErrors());
			throw new RestErrorException(message);
		}

		try {
			JsonResultResponse<ESDocument> alleles = alleleESService.getAllelesByGene(id, pagination);
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
	public JsonResultResponse<SequenceSummaryDocument> getAllelesVariantPerGene(String id, Integer limit, Integer page, String sortBy, String asc, String symbol, String associatedGeneSymbol, String synonyms, String hgvsgName, String variantType, String molecularConsequence, String impact,
		String sequenceFeatureType, String sequenceFeature, String variantPolyphen, String variantSift, String hasDisease, String hasPhenotype, String category, String location) {
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
		pagination.addFieldFilter(FieldFilter.VARIANT_HGVS_G, hgvsgName);
		pagination.addFieldFilter(FieldFilter.VARIANT_LOCATION, location);
		if (pagination.hasErrors()) {
			RestErrorMessage message = new RestErrorMessage();
			message.setErrors(pagination.getErrors());
			throw new RestErrorException(message);
		}

		try {
			JsonResultResponse<SequenceSummaryDocument> alleles = geneService.getAllelesAndVariantInfo(id, pagination);
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
	public Response getAllelesVariantPerGeneDownload(String id, String symbol, String associatedGeneSymbol, String synonyms, String hgvsgName, String variantType, String molecularConsequence, String impact, String sequenceFeatureType, String sequenceFeature, String variantPolyphen,
		String variantSift, String hasDisease, String hasPhenotype, String category, String location) {
		int pageSize = 10000;
		int page = 1;
		List<SequenceSummaryDocument> allResults = new ArrayList<>();
		long total;
		do {
			JsonResultResponse<SequenceSummaryDocument> batch = getAllelesVariantPerGene(id, pageSize, page, null, null, symbol, associatedGeneSymbol, synonyms, hgvsgName, variantType, molecularConsequence, impact, sequenceFeatureType, sequenceFeature, variantPolyphen, variantSift, hasDisease,
				hasPhenotype, category, location);
			allResults.addAll(batch.getResults());
			total = batch.getTotal();
			page++;
		} while (allResults.size() < total);

		Response.ResponseBuilder responseBuilder = Response.ok(alleleTranslator.getAllSequenceSummaryDetailRows(allResults));
		APIServiceHelper.setDownloadHeader(id, EntityType.GENE, EntityType.ALLELESANDVARIANT, responseBuilder);
		return responseBuilder.build();
	}

	@Override
	public Response getAllelesPerGeneDownload(String id, String sortBy, String asc, String symbol, String synonym, String variant, String variantType, String molecularConsequence, String disease, String phenotype, String category) {

		JsonResultResponse<ESDocument> alleles = getAllelesPerGene(id, 150000, 1, sortBy, asc, symbol, synonym, variant, variantType, molecularConsequence, disease, phenotype, category);

		Response.ResponseBuilder responseBuilder = Response.ok(alleleTranslator.getAllRows(alleles.getResults()));
		APIServiceHelper.setDownloadHeader(id, EntityType.GENE, EntityType.ALLELE, responseBuilder);
		return responseBuilder.build();
	}

	@Override
	public JsonResultResponse<GeneGeneticInteractionDocument> getGeneticInteractions(String id, Integer limit, Integer page, String sortBy, String asc, String interactorGeneSymbol, String interactorSpecies, String source, String reference, String role, String geneticPerturbation,
		String interactorRole, String interactorGeneticPerturbation, String phenotypes, String interactionType, @Context UriInfo info) {
		long startTime = System.currentTimeMillis();

		if (StringUtils.isEmpty(sortBy)) {
			sortBy = "geneGeneticInteraction.geneGeneAssociationObject.geneSymbol.displayText.sort";
		}
		Pagination pagination = new Pagination(page, limit, sortBy, asc, new InteractionColumnFieldMapping());
		pagination.addFilterOption("geneGeneticInteraction.geneGeneAssociationObject.geneSymbol.displayText", interactorGeneSymbol);
		pagination.addFilterOption("geneGeneticInteraction.interactionIdORgeneGeneticInteraction.crossReferences.displayName", source);
		pagination.addFilterOption("geneGeneticInteraction.evidence.referenceID", reference);
		pagination.addFilterOption("geneGeneticInteraction.interactorARole.name.keyword", role);
		pagination.addFilterOption("geneGeneticInteraction.interactorAGeneticPerturbation.alleleSymbol.displayText", geneticPerturbation);
		pagination.addFilterOption("geneGeneticInteraction.interactorBRole.name.keyword", interactorRole);
		pagination.addFilterOption("geneGeneticInteraction.interactorBGeneticPerturbation.alleleSymbol.displayText", interactorGeneticPerturbation);
		pagination.addFilterOption("geneGeneticInteraction.phenotypesOrTraits", phenotypes);
		pagination.addFilterOption("geneGeneticInteraction.interactionType.name.keyword", interactionType);
		pagination.addFilterOption("geneGeneticInteraction.geneGeneAssociationObject.taxon.species.fullName.keyword", interactorSpecies);
		// Todo: needs to be made generic
		// pagination.validateFilterValues(info.getQueryParameters());
		if (pagination.hasErrors()) {
			RestErrorMessage message = new RestErrorMessage();
			message.setErrors(pagination.getErrors());
			throw new RestErrorException(message);
		}
		try {
			JsonResultResponse<GeneGeneticInteractionDocument> interactions = geneService.getGeneticInteractions(id, pagination);
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
	public Response getGeneticInteractionsDownload(String id, String sortBy, String asc, String interactorGeneSymbol, String interactorSpecies, String source, String reference, String role, String geneticPerturbation, String interactorRole, String interactorGeneticPerturbation, String phenotypes,
		String interactionType) {
		if (StringUtils.isEmpty(sortBy)) {
			sortBy = "geneGeneticInteraction.geneGeneAssociationObject.geneSymbol.displayText.sort";
		}
		Pagination pagination = new Pagination(1, 150000, sortBy, asc);
		pagination.addFilterOption("geneGeneticInteraction.geneGeneAssociationObject.geneSymbol.displayText", interactorGeneSymbol);
		pagination.addFilterOption("geneGeneticInteraction.interactionIdORgeneGeneticInteraction.crossReferences.displayName", source);
		pagination.addFilterOption("geneGeneticInteraction.evidence.referenceID", reference);
		pagination.addFilterOption("geneGeneticInteraction.interactorARole.name.keyword", role);
		pagination.addFilterOption("geneGeneticInteraction.interactorAGeneticPerturbation.alleleSymbol.displayText", geneticPerturbation);
		pagination.addFilterOption("geneGeneticInteraction.interactorBRole.name.keyword", interactorRole);
		pagination.addFilterOption("geneGeneticInteraction.interactorBGeneticPerturbation.alleleSymbol.displayText", interactorGeneticPerturbation);
		pagination.addFilterOption("geneGeneticInteraction.phenotypesOrTraits", phenotypes);
		pagination.addFilterOption("geneGeneticInteraction.interactionType.name.keyword", interactionType);
		pagination.addFilterOption("geneGeneticInteraction.geneGeneAssociationObject.taxon.species.fullName.keyword", interactorSpecies);

		JsonResultResponse<GeneGeneticInteractionDocument> interactions = geneService.getGeneticInteractions(id, pagination);

		Response.ResponseBuilder responseBuilder = Response.ok(geneticInteractionTranslator.getAllRows(interactions.getResults()));
		APIServiceHelper.setDownloadHeader(id, EntityType.GENE, EntityType.INTERACTION, "genetic_interaction", responseBuilder);
		return responseBuilder.build();
	}

	@Override
	public JsonResultResponse<GeneMolecularInteractionDocument> getMolecularInteractions(String id, Integer limit, Integer page, String sortBy, String asc, String moleculeType, String interactorGeneSymbol, String interactorSpecies, String interactorMoleculeType, String detectionMethod,
		String source, String reference, @Context UriInfo info) {
		long startTime = System.currentTimeMillis();
		if (StringUtils.isEmpty(sortBy)) {
			sortBy = "geneMolecularInteraction.geneGeneAssociationObject.geneSymbol.displayText.sort";
		}
		Pagination pagination = new Pagination(page, limit, sortBy, asc, new InteractionColumnFieldMapping());
		pagination.addFilterOption("geneMolecularInteraction.interactorAType.name.keyword", moleculeType);
		pagination.addFilterOption("geneMolecularInteraction.geneGeneAssociationObject.geneSymbol.displayText", interactorGeneSymbol);
		pagination.addFilterOption("geneMolecularInteraction.interactionIdORgeneMolecularInteraction.aggregationDatabase.nameORgeneMolecularInteraction.interactionSource.nameORgeneMolecularInteraction.crossReferences.displayName", source);
		pagination.addFilterOption("geneMolecularInteraction.evidence.referenceID", reference);
		pagination.addFilterOption("geneMolecularInteraction.interactorBType.name.keyword", interactorMoleculeType);
		pagination.addFilterOption("geneMolecularInteraction.detectionMethod.name.keyword", detectionMethod);
		pagination.addFilterOption("geneMolecularInteraction.geneGeneAssociationObject.taxon.species.fullName.keyword", interactorSpecies);
		// Todo: needs to be made generic
		// pagination.validateFilterValues(info.getQueryParameters());
		if (pagination.hasErrors()) {
			RestErrorMessage message = new RestErrorMessage();
			message.setErrors(pagination.getErrors());
			throw new RestErrorException(message);
		}
		try {
			JsonResultResponse<GeneMolecularInteractionDocument> interactions = geneService.getMolecularInteractions(id, pagination);
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
	public Response getMolecularInteractionsDownload(String id, String sortBy, String asc, String moleculeType, String interactorGeneSymbol, String interactorSpecies, String interactorMoleculeType, String detectionMethod, String source, String reference) {
		if (StringUtils.isEmpty(sortBy)) {
			sortBy = "geneMolecularInteraction.geneGeneAssociationObject.geneSymbol.displayText.sort";
		}
		Pagination pagination = new Pagination(1, 150000, sortBy, asc);
		pagination.addFilterOption("geneMolecularInteraction.interactorAType.name.keyword", moleculeType);
		pagination.addFilterOption("geneMolecularInteraction.geneGeneAssociationObject.geneSymbol.displayText", interactorGeneSymbol);
		pagination.addFilterOption("geneMolecularInteraction.interactionIdORgeneMolecularInteraction.aggregationDatabase.nameORgeneMolecularInteraction.interactionSource.nameORgeneMolecularInteraction.crossReferences.displayName", source);
		pagination.addFilterOption("geneMolecularInteraction.evidence.referenceID", reference);
		pagination.addFilterOption("geneMolecularInteraction.interactorBType.name.keyword", interactorMoleculeType);
		pagination.addFilterOption("geneMolecularInteraction.detectionMethod.name.keyword", detectionMethod);
		pagination.addFilterOption("geneMolecularInteraction.geneGeneAssociationObject.taxon.species.fullName.keyword", interactorSpecies);
		JsonResultResponse<GeneMolecularInteractionDocument> interactions = geneService.getMolecularInteractions(id, pagination);

		Response.ResponseBuilder responseBuilder = Response.ok(molecularInteractionTranslator.getAllRows(interactions.getResults()));
		APIServiceHelper.setDownloadHeader(id, EntityType.GENE, EntityType.INTERACTION, "molecular_interaction", responseBuilder);
		return responseBuilder.build();
	}

	@Override
	public JsonResultResponse<GenePhenotypeAnnotationDocument> getPhenotypeAnnotations(String id, Integer limit, Integer page, String sortBy, String geneticEntity, String geneticEntityType, String phenotype, String reference, String dataProvider, String asc) {
		long startTime = System.currentTimeMillis();
		Pagination pagination = new Pagination(page, limit, sortBy, asc);
		pagination.addFilterOption("phenotypeStatement", phenotype);
		pagination.addFilterOption("pubmedPubModIDs", reference);
		pagination.addFilterOption("primaryAnnotations.dataProvider.abbreviation", dataProvider);
		try {
			JsonResultResponse<GenePhenotypeAnnotationDocument> phenotypes = phenotypeESService.getGenePhenotypeAnnotations(id, pagination, false);
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
	public Response getPhenotypeAnnotationsDownloadFile(String id, String sortBy, String geneticEntity, String geneticEntityType, String phenotype, String reference, String dataProvider, String asc) {
		// retrieve all records
		JsonResultResponse<GenePhenotypeAnnotationDocument> response = getPhenotypeAnnotations(id, 250000, 1, sortBy, geneticEntity, geneticEntityType, phenotype, reference, dataProvider, asc);
		Response.ResponseBuilder responseBuilder = Response.ok(translator.getAllRows(response.getResults()));
		APIServiceHelper.setDownloadHeader(id, EntityType.GENE, EntityType.PHENOTYPE, responseBuilder);
		return responseBuilder.build();
	}

	@Override
	public JsonResultResponse<AGMAnnotationDocument> getPrimaryAnnotatedEntityForModel(String id, Integer limit, Integer page, String sortBy, String modelName, String species, String experimentalCondition, String disease, String phenotype, String source, String asc) {
		long startTime = System.currentTimeMillis();
		Pagination pagination = new Pagination(page, limit, sortBy, asc);
		if (pagination.hasErrors()) {
			RestErrorMessage message = new RestErrorMessage();
			message.setErrors(pagination.getErrors());
			throw new RestErrorException(message);
		}
		pagination.addFilterOption("model.agmFullName.formatText", modelName);
		pagination.addFilterOption("diseaseModels.disease.name", disease);
		pagination.addFilterOption("conditionRelations.conditions.conditionSummary", experimentalCondition);
		pagination.addFilterOption("associatedPhenotype", phenotype);
		pagination.addFilterOption("dataProvider", source);

		try {
			JsonResultResponse<AGMAnnotationDocument> response = agmESService.getGeneAGMAnnotationDocuments(id, pagination, false);
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

	@Override
	public JsonResultResponse<GeneToGeneOrthologyDocument> getGeneOrthology(String id, List<String> geneIDs, String geneLister, String stringencyFilter, String taxonID, String method, Integer limit, Integer page) {

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
		final JsonResultResponse<GeneToGeneOrthologyDocument> response = orthologyESService.getOrthologyList(id, pagination);
		response.setHttpServletRequest(null);
		return response;
	}

	@Override
	public JsonResultResponse<GeneToGeneParalogyDocument> getGeneParalogy(String id, List<String> geneIDs, String geneLister, String stringencyFilter, String taxonID, String method, Integer limit, Integer page) {

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
		final JsonResultResponse<GeneToGeneParalogyDocument> response = geneToGeneParalogyESService.getParalogyMultiGeneJson(geneList, pagination);
		response.setHttpServletRequest(null);
		return response;
	}

	@Override
	// the List passed in here is unmodifiable
	public DiseaseRibbonSummary getDiseaseRibbonSummary(String id, Boolean includeNegation, Boolean debug, List<String> geneIDs) {
		List<String> ids = new ArrayList<>();
		if (geneIDs != null) {
			ids.addAll(geneIDs);
		}
		if (!id.equals("*")) {
			ids.add(id);
		}

		try {
			return diseaseESService.getDiseaseRibbonSummary(ids, includeNegation, debug);
		} catch (Exception e) {
			log.error("Error while creating disease ribbon summary", e);
			RestErrorMessage error = new RestErrorMessage();
			error.addErrorMessage(e.getMessage());
			throw new RestErrorException(error);
		}
	}

	@Override
	public JsonResultResponse<GeneTransgenicAlleleSummaryDocument> getTransgenicAlleles(String geneID, Integer limit, Integer page, String sortBy, String alleleSymbol, String constructSymbol, String constructRegulatedGene, String constructTargetedGene, String constructExpressedGene, String species,
		String hasPhenotype, String hasDisease, UriInfo ui) {
		if (sortBy != null && sortBy.isBlank()) {
			sortBy = "transgenicAllele";
		}
		Pagination pagination = new Pagination(page, limit, sortBy, null);
		pagination.addFilterOption("alleleDocument.allele.taxon.species.fullName.keyword", species);
		pagination.addFilterOption("alleleDocument.allele.alleleSymbol.formatText", alleleSymbol);
		pagination.addFilterOption("alleleDocument.transgenicAlleleConstructs.construct.constructSymbol.formatText", constructSymbol);
		pagination.addFilterOption("alleleDocument.transgenicAlleleConstructs.regulatoryGenes.geneSymbol.formatText", constructRegulatedGene);
		pagination.addFilterOption("alleleDocument.transgenicAlleleConstructs.expressedGenes.geneSymbol.formatText", constructExpressedGene);
		pagination.addFilterOption("alleleDocument.transgenicAlleleConstructs.targetedGenes.geneSymbol.formatText", constructTargetedGene);
		pagination.addFilterOption("alleleDocument.hasDiseaseAnnotations", hasDisease);
		pagination.addFilterOption("alleleDocument.hasPhenotypeAnnotations", hasPhenotype);
		if (pagination.hasErrors()) {
			RestErrorMessage message = new RestErrorMessage();
			message.setErrors(pagination.getErrors());
			throw new RestErrorException(message);
		}
		try {
			JsonResultResponse<GeneTransgenicAlleleSummaryDocument> response = transgenicAlleleESService.getTransgenicAlleles(geneID, pagination, false);
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
	public Response getTransgenicAllelesPerGeneDownload(String geneId, String sortBy, String alleleSymbol, String constructSymbol, String constructRegulatedGene, String constructTargetedGene, String constructExpressedGene, String species, String hasPhenotype, String hasDisease, UriInfo ui) {
		JsonResultResponse<GeneTransgenicAlleleSummaryDocument> alleles = getTransgenicAlleles(geneId, 20_000, 1, sortBy, alleleSymbol, constructSymbol, constructRegulatedGene, constructTargetedGene, constructExpressedGene, species, hasPhenotype, hasDisease, ui);

		Response.ResponseBuilder responseBuilder = Response.ok(alleleTranslator.getAllTransgenicAlleleRows(alleles.getResults()));
		APIServiceHelper.setDownloadHeader(geneId, EntityType.GENE, EntityType.TRANSGENICALLELE, responseBuilder);
		return responseBuilder.build();
	}


}
