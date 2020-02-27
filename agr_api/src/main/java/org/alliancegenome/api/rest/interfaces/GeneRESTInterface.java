package org.alliancegenome.api.rest.interfaces;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.alliancegenome.api.entity.DiseaseRibbonSummary;
import org.alliancegenome.api.entity.ExpressionSummary;
import org.alliancegenome.core.service.JsonResultResponse;
import org.alliancegenome.neo4j.entity.*;
import org.alliancegenome.neo4j.entity.node.Allele;
import org.alliancegenome.neo4j.entity.node.Gene;
import org.alliancegenome.neo4j.entity.node.InteractionGeneJoin;
import org.alliancegenome.neo4j.view.OrthologView;
import org.alliancegenome.neo4j.view.View;
import org.alliancegenome.neo4j.view.View.GeneAPI;
import org.alliancegenome.neo4j.view.View.GeneAllelesAPI;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.util.List;

@Path("/gene")
@Api(value = "Genes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface GeneRESTInterface {

    @GET
    @Path("/{id}")
    @JsonView(value = {GeneAPI.class})
    @ApiOperation(value = "Retrieve a Gene for given ID")
    Gene getGene(
            @ApiParam(name = "id", value = "Gene by ID")
            @PathParam("id") String id
    );

    @GET
    @Path("/{id}/alleles")
    @ApiOperation(value = "Retrieve all alleles of a given gene")
    @JsonView(value = {GeneAllelesAPI.class})
    JsonResultResponse<Allele> getAllelesPerGene(
            @ApiParam(name = "id", value = "Search for Alleles for a given Gene by ID")
            @PathParam("id") String id,
            @ApiParam(name = "limit", value = "Number of rows returned", defaultValue = "20")
            @DefaultValue("20") @QueryParam("limit") int limit,
            @ApiParam(name = "page", value = "Page number")
            @DefaultValue("1") @QueryParam("page") int page,
            @ApiParam(value = "Field name by which to sort", allowableValues = "symbol,name")
            @QueryParam("sortBy") String sortBy,
            @ApiParam(value = "ascending order: true or false", allowableValues = "true,false", defaultValue = "true")
            @QueryParam("asc") String asc,
            @ApiParam(name = "filter.symbol", value = "symbol of allele")
            @QueryParam("filter.symbol") String symbol,
            @ApiParam(name = "filter.synonym", value = "Allele synonyms")
            @QueryParam("filter.synonym") String synonym,
            @ApiParam(name = "filter.variantType", value = "Variant types")
            @QueryParam("filter.variantType") String variantType,
            @ApiParam(name = "filter.variantConsequence", value = "Consequence")
            @QueryParam("filter.variantConsequence") String consequence,
            @ApiParam(name = "filter.phenotype", value = "Phenotypes")
            @QueryParam("filter.phenotype") String phenotype,
            @ApiParam(value = "source")
            @QueryParam("filter.source") String source,
            @ApiParam(value = "Disease for a given allele")
            @QueryParam("filter.disease") String disease
    );

    //    @GET
    @Path("/{id}/alleles/download")
    @ApiOperation(value = "Retrieve all alleles for a given gene", hidden = true)
    @Produces(MediaType.TEXT_PLAIN)
    Response getAllelesPerGeneDownload(
            @ApiParam(name = "id", value = "Search for Alleles for a given Gene by ID")
            @PathParam("id") String id,
            @ApiParam(value = "Field name by which to sort", allowableValues = "symbol,name")
            @DefaultValue("symbol") @QueryParam("sortBy") String sortBy,
            @ApiParam(value = "ascending order: true or false", allowableValues = "true,false", defaultValue = "true")
            @QueryParam("asc") String asc,
            @ApiParam(name = "filter.symbol", value = "symbol of allele")
            @QueryParam("filter.symbol") String symbol,
            @ApiParam(name = "filter.synonym", value = "Allele synonyms")
            @QueryParam("filter.synonym") String synonym,
            @ApiParam(name = "filter.variantType", value = "Variant types")
            @QueryParam("filter.variantType") String variantType,
            @ApiParam(name = "filter.variantConsequence", value = "Consequence")
            @QueryParam("filter.variantConsequence") String consequence,
            @ApiParam(name = "filter.phenotype", value = "Phenotypes")
            @QueryParam("filter.phenotype") String phenotype,
            @ApiParam(value = "source")
            @QueryParam("filter.source") String source,
            @ApiParam(value = "Disease for a given allele")
            @QueryParam("filter.disease") String disease
    );

    @GET
    @Path("/{id}/phenotypes")
    @JsonView(value = {View.PhenotypeAPI.class})
    @ApiOperation(value = "Retrieve phenotype term name annotations for a given gene")
    JsonResultResponse<PhenotypeAnnotation> getPhenotypeAnnotations(
            @ApiParam(name = "id", value = "Gene by ID: e.g. ZFIN:ZDB-GENE-990415-8", required = true, type = "String")
            @PathParam("id") String id,
            @ApiParam(name = "limit", value = "Number of rows returned", defaultValue = "20")
            @DefaultValue("20") @QueryParam("limit") int limit,
            @ApiParam(name = "page", value = "Page number")
            @DefaultValue("1") @QueryParam("page") int page,
            @ApiParam(value = "Field name by which to sort", allowableValues = "termName,geneticEntity")
            @DefaultValue("termName") @QueryParam("sortBy") String sortBy,
            @ApiParam(name = "geneticEntity", value = "genetic entity symbol")
            @QueryParam("filter.geneticEntity") String geneticEntity,
            @ApiParam(name = "geneticEntityType", value = "genetic entity type", allowableValues = "allele,gene")
            @QueryParam("filter.geneticEntityType") String geneticEntityType,
            @ApiParam(value = "termName annotation")
            @QueryParam("filter.termName") String phenotype,
            @ApiParam(value = "Reference number: PUBMED or a Pub ID from the MOD")
            @QueryParam("filter.reference") String reference,
            @ApiParam(value = "ascending order: true or false", allowableValues = "true,false", defaultValue = "true")
            @QueryParam("asc") String asc);

    @GET
    @Path("/{id}/phenotypes/download")
    @ApiOperation(value = "Retrieve all termName annotations for a given gene", notes = "Download all termName annotations for a given gene")
    @Produces(MediaType.TEXT_PLAIN)
    Response getPhenotypeAnnotationsDownloadFile(
            @ApiParam(name = "id", value = "Gene by ID", required = true, type = "String")
            @PathParam("id") String id,
            @ApiParam(value = "Field name by which to sort", allowableValues = "termName,geneticEntity")
            @DefaultValue("termName") @QueryParam("sortBy") String sortBy,
            @ApiParam(name = "geneticEntity", value = "genetic entity symbol")
            @QueryParam("filter.geneticEntity") String geneticEntity,
            @ApiParam(name = "geneticEntityType", value = "genetic entity type", allowableValues = "allele")
            @QueryParam("filter.geneticEntityType") String geneticEntityType,
            @ApiParam(value = "termName annotation")
            @QueryParam("filter.termName") String phenotype,
            @ApiParam(value = "Reference number: PUBMED or a Pub ID from the MOD")
            @QueryParam("filter.reference") String reference,
            @ApiParam(value = "ascending order: true or false", allowableValues = "true,false", defaultValue = "true")
            @QueryParam("asc") String asc);

    @GET
    @Path("/{id}/models")
    @JsonView(value = {View.DiseaseAnnotationSummary.class})
    @ApiOperation(value = "Retrieve all DiseaseAnnotation records for a given disease id")
    JsonResultResponse<PrimaryAnnotatedEntity> getPrimaryAnnotatedEntityForModel(
            @ApiParam(name = "id", value = "gene ID: e.g. MGI:109583", required = true, type = "String")
            @PathParam("id") String id,
            @ApiParam(name = "limit", value = "Number of rows returned", defaultValue = "20")
            @DefaultValue("20") @QueryParam("limit") int limit,
            @ApiParam(name = "page", value = "Page number")
            @DefaultValue("1") @QueryParam("page") int page,
            @ApiParam(value = "Field / column name by which to sort", allowableValues = "Default,Gene,Disease,Species", defaultValue = "geneName")
            @QueryParam("sortBy") String sortBy,
            @ApiParam(value = "filter by model name")
            @QueryParam("filter.modelName") String modelName,
            @ApiParam(value = "filter by species")
            @QueryParam("filter.species") String species,
            @ApiParam(value = "filter by disease")
            @QueryParam("filter.disease") String disease,
            @ApiParam(value = "filter by phenotype")
            @QueryParam("filter.phenotype") String phenotype,
            @ApiParam(value = "filter by source")
            @QueryParam("filter.source") String source,
            @ApiParam(value = "ascending order: true or false", allowableValues = "true,false", defaultValue = "true")
            @QueryParam("asc") String asc);


    @GET
    @Path("/{id}/homologs")
    @JsonView(value = {View.Orthology.class})
    @ApiOperation(value = "Retrieve homologous gene records", notes = "Download homology records.")
    JsonResultResponse<OrthologView> getGeneOrthology(@ApiParam(name = "id", value = "Source Gene ID: the gene for which you are searching homologous gene, e.g. 'MGI:109583'", required = true, type = "String")
                                                      @PathParam("id") String id,
                                                      @ApiParam(name = "geneId", value = "List of additional source gene IDs for which homology is retrieved.")
                                                      @QueryParam("geneId") List<String> geneID,
                                                      @ApiParam(name = "geneIdList", value = "List of additional source gene IDs for which homology is retrieved in a comma-delimited list, e.g. 'MGI:109583,RGD:2129,MGI:97570'")
                                                      @QueryParam("geneIdList") String geneList,
                                                      @ApiParam(value = "apply stringency containsFilterValue", allowableValues = "stringent, moderate, all", defaultValue = "stringent")
                                                      @DefaultValue("stringent") @QueryParam("filter.stringency") String stringencyFilter,
                                                      @ApiParam(name = "taxonID", value = "Species identifier: Could be the full ID, e.g. 'NCBITaxon:10090', or just the ID, i.e. '10090'. Alternatively, part of a species name uniquely identifying a single species, e.g. 'danio' or 'mus'.", type = "String")
                                                      @QueryParam("filter.taxonID") String taxonID,
                                                      @ApiParam(value = "calculation methods", allowableValues = "Ensembl Compara, HGNC, Hieranoid, InParanoid, OMA, OrthoFinder, OrthoInspector, PANTHER, PhylomeDB, Roundup, TreeFam, ZFIN")
                                                      @QueryParam("filter.method") String method,
                                                      @ApiParam(value = "maximum number of rows returned")
                                                      @QueryParam("limit") Integer rows,
                                                      @ApiParam(value = "starting row number (for pagination)")
                                                      @DefaultValue("1") @QueryParam("page") Integer start) throws IOException;

    @GET
    @Path("/{id}/homologs-with-expression")
    @JsonView(value = {View.Orthology.class})
    @ApiOperation(value = "Retrieve homologous gene records that have expression data", notes = "Download homology records.")
    JsonResultResponse<OrthologView> getGeneOrthologyWithExpression(@ApiParam(name = "id", value = "Source Gene ID: the gene for which you are searching homologous gene, e.g. 'MGI:109583'", required = true, type = "String")
                                                                    @PathParam("id") String id,
                                                                    @ApiParam(value = "apply stringency containsFilterValue", allowableValues = "stringent, moderate, all", defaultValue = "stringent")
                                                                    @DefaultValue("stringent") @QueryParam("stringencyFilter") String stringencyFilter);

    @GET
    @Path("/{id}/interactions")
    @ApiOperation(value = "Retrieve interactions for a given gene")
    @JsonView(value = {View.Interaction.class})
    JsonResultResponse<InteractionGeneJoin> getInteractions(
            @ApiParam(name = "id", value = "Gene ID", required = true)
            @PathParam("id") String id,
            @ApiParam(name = "limit", value = "Number of rows returned", defaultValue = "20")
            @DefaultValue("20") @QueryParam("limit") int limit,
            @ApiParam(name = "page", value = "Page number")
            @DefaultValue("1") @QueryParam("page") int page,
            @ApiParam(value = "Name by which to sort", allowableValues = "interactorGeneSymbol,interactorMoleculeType,interactorSpecies,interactorSpecies,reference")
            @QueryParam("sortBy") String sortBy,
            @ApiParam(value = "ascending order: true or false", allowableValues = "true,false", defaultValue = "true")
            @QueryParam("asc") String asc,
            @ApiParam(name = "filter.moleculeType", value = "molecule type")
            @QueryParam("filter.moleculeType") String moleculeType,
            @ApiParam(name = "filter.interactorGeneSymbol", value = "gene symbol")
            @QueryParam("filter.interactorGeneSymbol") String interactorGeneSymbol,
            @ApiParam(name = "filter.interactorSpecies", value = "species")
            @QueryParam("filter.interactorSpecies") String interactorSpecies,
            @ApiParam(name = "filter.interactorMoleculeType", value = "molecule type")
            @QueryParam("filter.interactorMoleculeType") String interactorMoleculeType,
            @ApiParam(name = "filter.detectionMethod", value = "detection method")
            @QueryParam("filter.detectionMethod") String detectionMethod,
            @ApiParam(name = "filter.source", value = "database")
            @QueryParam("filter.source") String source,
            @ApiParam(name = "filter.reference", value = "References")
            @QueryParam("filter.reference") String reference,
            @Context UriInfo info);

    @GET
    @Path("/{id}/interactions/download")
    @ApiOperation(value = "Retrieve interactions for a given gene")
    @Produces(MediaType.TEXT_PLAIN)
    Response getInteractionsDownload(
            @ApiParam(name = "id", value = "Gene ID", required = true)
            @PathParam("id") String id,
            @ApiParam(value = "Name by which to sort", allowableValues = "interactorGeneSymbol,interactorMoleculeType,interactorSpecies,interactorSpecies,reference")
            @QueryParam("sortBy") String sortBy,
            @ApiParam(value = "ascending order: true or false", allowableValues = "true,false", defaultValue = "true")
            @QueryParam("asc") String asc,
            @ApiParam(name = "filter.moleculeType", value = "molecule type")
            @QueryParam("filter.moleculeType") String moleculeType,
            @ApiParam(name = "filter.interactorGeneSymbol", value = "gene symbol")
            @QueryParam("filter.interactorGeneSymbol") String interactorGeneSymbol,
            @ApiParam(name = "filter.interactorSpecies", value = "species")
            @QueryParam("filter.interactorSpecies") String interactorSpecies,
            @ApiParam(name = "filter.interactorMoleculeType", value = "molecule type")
            @QueryParam("filter.interactorMoleculeType") String interactorMoleculeType,
            @ApiParam(name = "filter.detectionMethod", value = "detection method")
            @QueryParam("filter.detectionMethod") String detectionMethod,
            @ApiParam(name = "filter.source", value = "database")
            @QueryParam("filter.source") String source,
            @ApiParam(name = "filter.reference", value = "References")
            @QueryParam("filter.reference") String reference
    );

    @GET
    @Path("/{id}/expression-summary")
    @JsonView(value = {View.Expression.class})
    @ApiOperation(value = "Retrieve all expression records of a given gene")
    ExpressionSummary getExpressionSummary(
            @ApiParam(name = "id", value = "Gene by ID, e.g. 'RGD:2129' or 'ZFIN:ZDB-GENE-990415-72 fgf8a'", required = true, type = "String")
            @PathParam("id") String id
    ) throws JsonProcessingException;

    @GET
    @Path("/{id}/disease-ribbon-summary")
    @JsonView(value = {View.DiseaseAnnotation.class})
    @ApiOperation(value = "Retrieve all disease records of a given gene")
    DiseaseRibbonSummary getDiseaseSummary(
            @ApiParam(name = "id", value = "Gene by ID, e.g. 'RGD:2129' or 'ZFIN:ZDB-GENE-990415-72 fgf8a'", required = true, type = "String")
            @PathParam("id") String id,
            @ApiParam(name = "geneID", value = "additional orthologous genes", required = true)
            @QueryParam("geneID") List<String> geneIDs
    ) throws JsonProcessingException;

    @GET
    @Path("/{id}/interaction-summary")
    @JsonView(value = {View.Expression.class})
    @ApiOperation(value = "Retrieve interaction summary records of a given gene")
    EntitySummary getInteractionSummary(
            @ApiParam(name = "id", value = "Gene by ID, e.g. 'RGD:2129' or 'ZFIN:ZDB-GENE-990415-72 fgf8a'", required = true, type = "String")
            @PathParam("id") String id
    ) throws JsonProcessingException;


    @GET
    @Path("/{id}/diseases-by-experiment")
    @ApiOperation(value = "Retrieve disease annotations for a given gene")
    @JsonView(value = {View.DiseaseAnnotation.class})
    JsonResultResponse<DiseaseAnnotation> getDiseaseByExperiment(
            @ApiParam(name = "id", value = "Gene by ID: e.g. ZFIN:ZDB-GENE-990415-8", required = true, type = "String")
            @PathParam("id") String id,
            @ApiParam(name = "limit", value = "Number of rows returned", defaultValue = "20")
            @DefaultValue("20") @QueryParam("limit") int limit,
            @ApiParam(name = "page", value = "Page number")
            @DefaultValue("1") @QueryParam("page") int page,
            @ApiParam(value = "Field name by which to sort", allowableValues = "disease,geneticEntity")
            @DefaultValue("disease") @QueryParam("sortBy") String sortBy,
            @ApiParam(name = "geneticEntity", value = "genetic entity symbol")
            @QueryParam("filter.geneticEntity") String geneticEntity,
            @ApiParam(name = "geneticEntityType", value = "genetic entity type", allowableValues = "allele,gene")
            @QueryParam("filter.geneticEntityType") String geneticEntityType,
            @ApiParam(value = "termName annotation")
            @QueryParam("filter.disease") String phenotype,
            @ApiParam(value = "association type")
            @QueryParam("filter.associationType") String associationType,
            @ApiParam(value = "Evidence Code")
            @QueryParam("filter.evidenceCode") String evidenceCode,
            @ApiParam(value = "Data Source")
            @QueryParam("source") String source,
            @ApiParam(value = "Reference number: PUBMED or a Pub ID from the MOD")
            @QueryParam("publications") String reference,
            @ApiParam(value = "ascending order: true or false", allowableValues = "true,false", defaultValue = "true")
            @QueryParam("asc") String asc,
            @Context UriInfo ui) throws JsonProcessingException;

    @GET
    @Path("/{id}/diseases-by-experiment/download")
    @ApiOperation(value = "Retrieve all disease annotations for a given gene and containsFilterValue option", hidden = true)
    Response getDiseaseByExperimentDownload(
            @ApiParam(name = "id", value = "Gene by ID: e.g. MGI:1097693", required = true, type = "String")
            @PathParam("id") String id,
            @ApiParam(value = "Field name by which to sort", allowableValues = "disease,geneticEntity")
            @DefaultValue("disease") @QueryParam("sortBy") String sortBy,
            @ApiParam(name = "geneticEntity", value = "genetic entity symbol")
            @QueryParam("filter.geneticEntity") String geneticEntity,
            @ApiParam(name = "geneticEntityType", value = "genetic entity type", allowableValues = "allele,gene")
            @QueryParam("filter.geneticEntityType") String geneticEntityType,
            @ApiParam(value = "disease annotation")
            @QueryParam("filter.disease") String phenotype,
            @ApiParam(value = "association type")
            @QueryParam("filter.associationType") String associationType,
            @ApiParam(value = "Evidence Code")
            @QueryParam("filter.evidenceCode") String evidenceCode,
            @ApiParam(value = "Data Source")
            @QueryParam("filter.source") String source,
            @ApiParam(value = "Reference number: PUBMED or a Pub ID from the MOD")
            @QueryParam("filter.reference") String reference,
            @ApiParam(value = "ascending order: true or false", allowableValues = "true,false", defaultValue = "true")
            @QueryParam("asc") String asc,
            @Context UriInfo ui) throws JsonProcessingException;

    @GET
    @Path("/{id}/disease-summary")
    @ApiOperation(value = "Retrieve disease summary info for a given gene and disease type")
    DiseaseSummary getDiseaseSummary(
            @ApiParam(name = "id", value = "Gene by ID: e.g. MGI:1097693", required = true, type = "String")
            @PathParam("id") String id,
            @ApiParam(value = "type", allowableValues = "experiment,orthology", defaultValue = "experiment")
            @QueryParam("type") String type
    ) throws JsonProcessingException;

    @GET
    @Path("/{id}/phenotype-summary")
    @ApiOperation(value = "Retrieve phenotype summary info for a given gene")
    EntitySummary getPhenotypeSummary(
            @ApiParam(name = "id", value = "Gene by ID: e.g. MGI:1097693", required = true, type = "String")
            @PathParam("id") String id
    ) throws JsonProcessingException;
}
