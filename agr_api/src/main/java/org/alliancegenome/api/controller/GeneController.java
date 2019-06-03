package org.alliancegenome.api.controller;


import lombok.extern.log4j.Log4j2;
import org.alliancegenome.api.entity.DiseaseRibbonSummary;
import org.alliancegenome.api.entity.ExpressionSummary;
import org.alliancegenome.api.rest.interfaces.GeneRESTInterface;
import org.alliancegenome.api.service.*;
import org.alliancegenome.core.exceptions.RestErrorException;
import org.alliancegenome.core.exceptions.RestErrorMessage;
import org.alliancegenome.core.service.JsonResultResponse;
import org.alliancegenome.core.service.OrthologyService;
import org.alliancegenome.core.translators.tdf.AlleleToTdfTranslator;
import org.alliancegenome.core.translators.tdf.DiseaseAnnotationToTdfTranslator;
import org.alliancegenome.core.translators.tdf.InteractionToTdfTranslator;
import org.alliancegenome.core.translators.tdf.PhenotypeAnnotationToTdfTranslator;
import org.alliancegenome.es.model.query.FieldFilter;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.neo4j.entity.DiseaseAnnotation;
import org.alliancegenome.neo4j.entity.DiseaseSummary;
import org.alliancegenome.neo4j.entity.EntitySummary;
import org.alliancegenome.neo4j.entity.PhenotypeAnnotation;
import org.alliancegenome.neo4j.entity.node.Allele;
import org.alliancegenome.neo4j.entity.node.Gene;
import org.alliancegenome.neo4j.entity.node.InteractionGeneJoin;
import org.alliancegenome.neo4j.view.OrthologView;
import org.alliancegenome.neo4j.view.OrthologyFilter;
import org.apache.commons.collections.CollectionUtils;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequestScoped
@Log4j2
public class GeneController extends BaseController implements GeneRESTInterface {

    @Inject
    private GeneService geneService;

    @Inject
    private DiseaseService diseaseService;
    @Inject
    private HttpServletRequest request;

    private final PhenotypeAnnotationToTdfTranslator translator = new PhenotypeAnnotationToTdfTranslator();
    private final AlleleToTdfTranslator alleleTanslator = new AlleleToTdfTranslator();
    private final InteractionToTdfTranslator interactionTanslator = new InteractionToTdfTranslator();
    private final DiseaseAnnotationToTdfTranslator diseaseTranslator = new DiseaseAnnotationToTdfTranslator();

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
    public JsonResultResponse<Allele> getAllelesPerGene(String id, int limit, int page, String sortBy, String asc,
                                                        String symbol, String synonym, String source, String disease) {
        long startTime = System.currentTimeMillis();
        Pagination pagination = new Pagination(page, limit, sortBy, asc);
        pagination.addFieldFilter(FieldFilter.SYMBOL, symbol);
        pagination.addFieldFilter(FieldFilter.SYNONYMS, synonym);
        pagination.addFieldFilter(FieldFilter.SOURCE, source);
        pagination.addFieldFilter(FieldFilter.DISEASE, disease);
        if (pagination.hasErrors()) {
            RestErrorMessage message = new RestErrorMessage();
            message.setErrors(pagination.getErrors());
            throw new RestErrorException(message);
        }

        JsonResultResponse<Allele> alleles = geneService.getAlleles(id, pagination);
        alleles.setHttpServletRequest(request);
        alleles.calculateRequestDuration(startTime);
        return alleles;
    }

    @Override
    public Response getAllelesPerGeneDownload(String id, String sortBy, String asc,
                                              String symbol, String synonym, String source, String disease) {
        Pagination pagination = new Pagination(1, Integer.MAX_VALUE, sortBy, asc);
        pagination.addFieldFilter(FieldFilter.SYMBOL, symbol);
        pagination.addFieldFilter(FieldFilter.SYNONYMS, synonym);
        pagination.addFieldFilter(FieldFilter.SOURCE, source);
        pagination.addFieldFilter(FieldFilter.DISEASE, disease);
        if (pagination.hasErrors()) {
            RestErrorMessage message = new RestErrorMessage();
            message.setErrors(pagination.getErrors());
            throw new RestErrorException(message);
        }

        JsonResultResponse<Allele> alleles = geneService.getAlleles(id, pagination);

        Response.ResponseBuilder responseBuilder = Response.ok(alleleTanslator.getAllRows(alleles.getResults()));
        APIService.setDownloadHeader(id, EntityType.GENE, EntityType.ALLELE, responseBuilder);
        return responseBuilder.build();
    }

    @Override
    public JsonResultResponse<InteractionGeneJoin> getInteractions(String id, int limit, int page, String sortBy, String asc,
                                                                   String moleculeType,
                                                                   String interactorGeneSymbol,
                                                                   String interactorSpecies,
                                                                   String interactorMoleculeType,
                                                                   String detectionMethod,
                                                                   String source,
                                                                   String reference) {
        long startTime = System.currentTimeMillis();
        Pagination pagination = new Pagination(page, limit, sortBy, asc);
        pagination.addFieldFilter(FieldFilter.MOLECULE_TYPE, moleculeType);
        pagination.addFieldFilter(FieldFilter.INTERACTOR_GENE_SYMBOL, interactorGeneSymbol);
        pagination.addFieldFilter(FieldFilter.INTERACTOR_SPECIES, interactorSpecies);
        pagination.addFieldFilter(FieldFilter.INTERACTOR_MOLECULE_TYPE, interactorMoleculeType);
        pagination.addFieldFilter(FieldFilter.DETECTION_METHOD, detectionMethod);
        pagination.addFieldFilter(FieldFilter.SOURCE, source);
        pagination.addFieldFilter(FieldFilter.FREFERENCE, reference);
        JsonResultResponse<InteractionGeneJoin> interactions = geneService.getInteractions(id, pagination);
        interactions.setHttpServletRequest(request);
        interactions.calculateRequestDuration(startTime);
        return interactions;
    }

    @Override
    public Response getInteractionsDownload(String id, String sortBy, String asc,
                                            String moleculeType,
                                            String interactorGeneSymbol,
                                            String interactorSpecies,
                                            String interactorMoleculeType,
                                            String detectionMethod,
                                            String source,
                                            String reference) {
        Pagination pagination = new Pagination(1, Integer.MAX_VALUE, sortBy, asc);
        pagination.addFieldFilter(FieldFilter.MOLECULE_TYPE, moleculeType);
        pagination.addFieldFilter(FieldFilter.INTERACTOR_GENE_SYMBOL, interactorGeneSymbol);
        pagination.addFieldFilter(FieldFilter.INTERACTOR_SPECIES, interactorSpecies);
        pagination.addFieldFilter(FieldFilter.INTERACTOR_MOLECULE_TYPE, interactorMoleculeType);
        pagination.addFieldFilter(FieldFilter.DETECTION_METHOD, detectionMethod);
        pagination.addFieldFilter(FieldFilter.SOURCE, source);
        pagination.addFieldFilter(FieldFilter.FREFERENCE, reference);
        JsonResultResponse<InteractionGeneJoin> interactions = geneService.getInteractions(id, pagination);

        Response.ResponseBuilder responseBuilder = Response.ok(interactionTanslator.getAllRows(interactions.getResults()));
        APIService.setDownloadHeader(id, EntityType.GENE, EntityType.INTERACTION, responseBuilder);
        return responseBuilder.build();
    }

    @Override
    public JsonResultResponse<PhenotypeAnnotation> getPhenotypeAnnotations(String id, int limit, int page, String sortBy,
                                                                           String geneticEntity,
                                                                           String geneticEntityType,
                                                                           String phenotype,
                                                                           String reference,
                                                                           String asc) {
        return getPhenotypeAnnotationDocumentJsonResultResponse(id, limit, page, sortBy, geneticEntity, geneticEntityType, phenotype, reference, asc);
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
        APIService.setDownloadHeader(id, EntityType.GENE, EntityType.PHENOTYPE, responseBuilder);
        return responseBuilder.build();
    }

    private JsonResultResponse<PhenotypeAnnotation> getPhenotypeAnnotationDocumentJsonResultResponse(String id, int limit, int page, String sortBy, String geneticEntity, String geneticEntityType, String phenotype, String reference, String asc) {
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
        return getDiseaseAnnotation(id, limit, page, sortBy, geneticEntity, geneticEntityType, disease, associationType, reference, null, null, evidenceCode, source, asc, ui, true);
    }

    private JsonResultResponse<DiseaseAnnotation> getDiseaseViaOrthologyAnnotation(String id,
                                                                                   Integer limit,
                                                                                   Integer page,
                                                                                   String sortBy,
                                                                                   String orthologyGene,
                                                                                   String orthologyGeneSpecies,
                                                                                   String disease,
                                                                                   String associationType,
                                                                                   String evidenceCode,
                                                                                   String source,
                                                                                   String reference,
                                                                                   String asc,
                                                                                   UriInfo ui) {
        return getDiseaseAnnotation(id, limit, page, sortBy, null, null, disease, associationType, reference, orthologyGene, orthologyGeneSpecies, evidenceCode, source, asc, ui, false);
    }

    private JsonResultResponse<DiseaseAnnotation> getDiseaseAnnotation(String id,
                                                                       Integer limit,
                                                                       Integer page,
                                                                       String sortBy,
                                                                       String geneticEntity,
                                                                       String geneticEntityType,
                                                                       String disease,
                                                                       String associationType,
                                                                       String reference,
                                                                       String orthologyGene,
                                                                       String orthologyGeneSpecies,
                                                                       String evidenceCode,
                                                                       String source,
                                                                       String asc,
                                                                       UriInfo ui,
                                                                       boolean empiricalDisease) {
        Pagination pagination = new Pagination(page, limit, sortBy, asc);
        pagination.addFieldFilter(FieldFilter.GENETIC_ENTITY, geneticEntity);
        pagination.addFieldFilter(FieldFilter.GENETIC_ENTITY_TYPE, geneticEntityType);
        pagination.addFieldFilter(FieldFilter.ASSOCIATION_TYPE, associationType);
        pagination.addFieldFilter(FieldFilter.EVIDENCE_CODE, evidenceCode);
        pagination.addFieldFilter(FieldFilter.SOURCE, source);
        pagination.addFieldFilter(FieldFilter.ORTHOLOG, orthologyGene);
        pagination.addFieldFilter(FieldFilter.ORTHOLOG_SPECIES, orthologyGeneSpecies);
        pagination.addFieldFilter(FieldFilter.DISEASE, disease);
        pagination.addFieldFilter(FieldFilter.FREFERENCE, reference);
        MultivaluedMap<String, String> parameterMap = ui.getQueryParameters();
        List<String> invalidFilterNames = parameterMap.entrySet().stream()
                .filter(entry -> {
                    if (FieldFilter.hasFieldFilterPrefix(entry.getKey()) && !FieldFilter.isFieldFilterValue(entry.getKey()))
                        return true;
                    return false;
                })
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        pagination.setInvalidFilterList(invalidFilterNames);
        return diseaseService.getDiseaseAnnotations(id, pagination, empiricalDisease);
    }

    @Override
    public JsonResultResponse<OrthologView> getGeneOrthology(String id,
                                                             List<String> geneIDs,
                                                             String geneLister,
                                                             String stringencyFilter,
                                                             List<String> taxonIDs,
                                                             List<String> methods,
                                                             Integer rows,
                                                             Integer start) {

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
        OrthologyFilter orthologyFilter = new OrthologyFilter(stringencyFilter, taxonIDs, methods);
        if (rows != null && rows > 0) {
            orthologyFilter.setRows(rows);
        }
        orthologyFilter.setStart(start);
        return OrthologyService.getOrthologyMultiGeneJson(geneList, orthologyFilter);
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
        JsonResultResponse<OrthologView> orthologs = OrthologyService.getOrthologyGenes(geneList, orthologyFilter);
        List<OrthologView> filteredList = orthologs.getResults().stream()
                .filter(orthologView -> getExpressionSummary(orthologView.getHomologGene().getPrimaryKey()).hasData())
                .collect(Collectors.toList());
        orthologs.setResults(filteredList);
        orthologs.setTotal(filteredList.size());
        orthologs.setHttpServletRequest(request);
        orthologs.calculateRequestDuration(startTime);
        return orthologs;
    }

    @Override
    public ExpressionSummary getExpressionSummary(String id) {

        ExpressionService service = new ExpressionService();
        return service.getExpressionSummary(id);
    }

    @Override
    // the List passed in here is unmodifiable
    public DiseaseRibbonSummary getDiseaseSummary(String id, List<String> geneIDs) {
        List<String> ids = new ArrayList<>();
        if (geneIDs != null)
            ids.addAll(geneIDs);
        ids.add(id);
        DiseaseService service = new DiseaseService();
        try {
            return service.getDiseaseRibbonSummary(ids);
        } catch (Exception e) {
            log.error(e);
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
                                                                        int limit,
                                                                        int page,
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
    public JsonResultResponse<DiseaseAnnotation> getDiseaseViaOrthology(String id,
                                                                        int limit,
                                                                        int page,
                                                                        String sortBy,
                                                                        String orthologyGene,
                                                                        String orthologyGeneSpecies,
                                                                        String disease,
                                                                        String associationType,
                                                                        String evidenceCode,
                                                                        String source,
                                                                        String reference,
                                                                        String asc,
                                                                        UriInfo ui) {
        return getDiseaseViaOrthologyAnnotation(id,
                limit,
                page,
                sortBy,
                orthologyGene,
                orthologyGeneSpecies,
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
    public Response getDiseaseViaOrthologyDownload(String id,
                                                   String sortBy,
                                                   String orthologyGene,
                                                   String orthologyGeneSpecies,
                                                   String disease,
                                                   String associationType,
                                                   String evidenceCode,
                                                   String source,
                                                   String reference,
                                                   String asc,
                                                   UriInfo ui) {
        JsonResultResponse<DiseaseAnnotation> response = getDiseaseViaOrthologyAnnotation(id,
                Integer.MAX_VALUE,
                null,
                sortBy,
                orthologyGene,
                orthologyGeneSpecies,
                disease,
                associationType,
                evidenceCode,
                source,
                reference,
                asc,
                ui);
        Response.ResponseBuilder responseBuilder = Response.ok(diseaseTranslator.getDiseaseViaOrthologyByGene(response.getResults()));
        responseBuilder.type(MediaType.TEXT_PLAIN_TYPE);
        responseBuilder.header("Content-Disposition", "attachment; filename=\"DiseaseAssociationsViaOrthologyData-" + id.replace(":", "-") + ".tsv\"");
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

}
