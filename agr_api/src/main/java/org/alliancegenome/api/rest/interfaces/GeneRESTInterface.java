package org.alliancegenome.api.rest.interfaces;

import java.io.IOException;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.alliancegenome.api.entity.DiseaseRibbonSummary;
import org.alliancegenome.api.entity.ExpressionSummary;
import org.alliancegenome.cache.repository.helper.JsonResultResponse;
import org.alliancegenome.neo4j.entity.DiseaseAnnotation;
import org.alliancegenome.neo4j.entity.DiseaseSummary;
import org.alliancegenome.neo4j.entity.EntitySummary;
import org.alliancegenome.neo4j.entity.PhenotypeAnnotation;
import org.alliancegenome.neo4j.entity.PrimaryAnnotatedEntity;
import org.alliancegenome.neo4j.entity.node.Allele;
import org.alliancegenome.neo4j.entity.node.Gene;
import org.alliancegenome.neo4j.entity.node.InteractionGeneJoin;
import org.alliancegenome.neo4j.view.OrthologView;
import org.alliancegenome.neo4j.view.View;
import org.alliancegenome.neo4j.view.View.GeneAPI;
import org.alliancegenome.neo4j.view.View.GeneAllelesAPI;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.ParameterIn;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.core.JsonProcessingException;

@Path("/gene")
@Tag(name = "Genes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface GeneRESTInterface {

    @GET
    @Path("/{id}")
    @JsonView(value = {GeneAPI.class})
    @Operation(description = "Retrieve a Gene for given ID")
    Gene getGene(
            @Parameter(in=ParameterIn.PATH, name = "id", description = "Retrieve a Gene for given ID", required=true, schema = @Schema(type = SchemaType.STRING))
            @PathParam("id") String id
    );

    @GET
    @Path("/{id}/alleles")
    @Operation(summary = "Retrieve all alleles of a given gene")
    @JsonView(value = {GeneAllelesAPI.class})
    JsonResultResponse<Allele> getAllelesPerGene(
            //@ApiParam(name = "id", description = "Search for Alleles for a given Gene by ID")
            @Parameter(in=ParameterIn.PATH, name = "id", description = "Search for Alleles for a given Gene by ID", required=true, schema = @Schema(type = SchemaType.STRING))
            @PathParam("id") String id,
            @Parameter(in=ParameterIn.QUERY, name = "limit", description = "Number of rows returned", schema = @Schema(type = SchemaType.INTEGER))
            @DefaultValue("20") @QueryParam("limit") int limit,
            @Parameter(in=ParameterIn.QUERY, name = "page", description = "Page number", schema = @Schema(type = SchemaType.INTEGER))
            @DefaultValue("1") @QueryParam("page") int page,
            @Parameter(in=ParameterIn.QUERY, name = "sortBy", description = "Field name by which to sort", schema = @Schema(type = SchemaType.STRING))
            @DefaultValue("symbol") @QueryParam("sortBy") String sortBy,
            @Parameter(in=ParameterIn.QUERY, name = "asc", description = "order to sort by", schema = @Schema(type = SchemaType.STRING))
            @QueryParam("asc") String asc,
            @Parameter(in=ParameterIn.QUERY, name = "filter.symbol", description = "allele symbol", schema = @Schema(type = SchemaType.STRING))
            @QueryParam("filter.symbol") String symbol,
            @Parameter(in=ParameterIn.QUERY, name = "filter.synonym", description = "allele synonym", schema = @Schema(type = SchemaType.STRING))
            @QueryParam("filter.synonym") String synonym,
            @Parameter(in=ParameterIn.QUERY, name = "filter.variantType", description = "Variant types", schema = @Schema(type = SchemaType.STRING))
            @QueryParam("filter.variantType") String variantType,
            @Parameter(in=ParameterIn.QUERY, name = "filter.variantConsequence", description = "Consequence", schema = @Schema(type = SchemaType.STRING))
            @QueryParam("filter.variantConsequence") String consequence,
            @Parameter(in=ParameterIn.QUERY, name = "filter.phenotype", description = "Phenotypes", schema = @Schema(type = SchemaType.STRING))
            @QueryParam("filter.phenotype") String phenotype,
            @Parameter(in=ParameterIn.QUERY, name = "filter.source", description = "Source", schema = @Schema(type = SchemaType.STRING))
            @QueryParam("filter.source") String source,
            @Parameter(in=ParameterIn.QUERY, name = "filter.disease", description = "Disease for a given allele", schema = @Schema(type = SchemaType.STRING))
            @QueryParam("filter.disease") String disease
    );

    //    @GET
    @Path("/{id}/alleles/download")
    @Operation(summary = "Retrieve all alleles for a given gene", hidden = true)
    @Produces(MediaType.TEXT_PLAIN)
    Response getAllelesPerGeneDownload(
            @Parameter(in=ParameterIn.PATH, name = "id", description = "Search for Alleles for a given Gene by ID", required=true, schema = @Schema(type = SchemaType.STRING))
            @PathParam("id") String id,
            @Parameter(in=ParameterIn.QUERY, name = "sortBy", description = "Field name by which to sort", schema = @Schema(type = SchemaType.STRING))
            @DefaultValue("symbol") @QueryParam("sortBy") String sortBy,
            @Parameter(in = ParameterIn.QUERY, name = "asc", description = "order to sort by", schema = @Schema(type = SchemaType.STRING))
            @DefaultValue("true") @QueryParam("asc") String asc,
            @Parameter(in=ParameterIn.QUERY, name = "filter.symbol", description = "allele symbol", schema = @Schema(type = SchemaType.STRING))
            @QueryParam("filter.symbol") String symbol,
            @Parameter(in=ParameterIn.QUERY, name = "filter.synonym", description = "allele synonym", schema = @Schema(type = SchemaType.STRING))
            @QueryParam("filter.synonym") String synonym,
            @Parameter(in=ParameterIn.QUERY, name = "filter.variantType", description = "Variant types", schema = @Schema(type = SchemaType.STRING))
            @QueryParam("filter.variantType") String variantType,
            @Parameter(in=ParameterIn.QUERY, name = "filter.variantConsequence", description = "Consequence", schema = @Schema(type = SchemaType.STRING))
            @QueryParam("filter.variantConsequence") String consequence,
            @Parameter(in=ParameterIn.QUERY, name = "filter.phenotype", description = "Phenotypes", schema = @Schema(type = SchemaType.STRING))
            @QueryParam("filter.phenotype") String phenotype,
            @Parameter(in=ParameterIn.QUERY, name = "filter.source", description = "Source", schema = @Schema(type = SchemaType.STRING))
            @QueryParam("filter.source") String source,
            @Parameter(in=ParameterIn.QUERY, name = "filter.disease", description = "Disease for a given allele", schema = @Schema(type = SchemaType.STRING))
            @QueryParam("filter.disease") String disease
    );

    @GET
    @Path("/{id}/phenotypes")
    @JsonView(value = {View.PhenotypeAPI.class})
    @Operation(summary = "Retrieve phenotype term name annotations for a given gene")
    JsonResultResponse<PhenotypeAnnotation> getPhenotypeAnnotations(
            @Parameter(in=ParameterIn.PATH, name = "id", description = "Gene by ID: e.g. ZFIN:ZDB-GENE-990415-8", required=true, schema = @Schema(type = SchemaType.STRING))
            @PathParam("id") String id,
            @Parameter(in=ParameterIn.QUERY, name = "limit", description = "Number of rows returned", schema = @Schema(type = SchemaType.INTEGER))
            @DefaultValue("20") @QueryParam("limit") int limit,
            @Parameter(in=ParameterIn.QUERY, name = "page", description = "Page number", schema = @Schema(type = SchemaType.INTEGER))
            @DefaultValue("1") @QueryParam("page") int page,
            @Parameter(in=ParameterIn.QUERY, name = "sortBy", description = "Field name by which to sort", schema = @Schema(type = SchemaType.STRING))
            @DefaultValue("term") @QueryParam("sortBy") String sortBy,
            @Parameter(in=ParameterIn.QUERY, name = "filter.geneticEntity", description = "genetic entity symbol", schema = @Schema(type = SchemaType.STRING))
            @QueryParam("filter.geneticEntity") String geneticEntity,
            @Parameter(in=ParameterIn.QUERY, name = "filter.geneticEntityType", description = "genetic entity type", schema = @Schema(type = SchemaType.STRING))
            //allowedValues = "allele,gene"
            @QueryParam("filter.geneticEntityType") String geneticEntityType,
            @Parameter(in=ParameterIn.QUERY, name = "filter.termName", description = "term name", schema = @Schema(type = SchemaType.STRING))
            @QueryParam("filter.termName") String phenotype,
            @Parameter(in=ParameterIn.QUERY, name = "filter.reference", description = "Reference number: PUBMED or a Pub ID from the MOD", schema = @Schema(type = SchemaType.STRING))
            @QueryParam("filter.reference") String reference,
            @Parameter(in=ParameterIn.QUERY, name = "asc", description = "order to sort by", schema = @Schema(type = SchemaType.STRING))
            @DefaultValue("true") @QueryParam("asc") String asc);

    @GET
    @Path("/{id}/phenotypes/download")
    @Operation(summary = "Download all termName annotations for a given gene")
    @Produces(MediaType.TEXT_PLAIN)
    Response getPhenotypeAnnotationsDownloadFile(
            @Parameter(in=ParameterIn.PATH, name = "id", description = "Gene by ID", required=true, schema = @Schema(type = SchemaType.STRING))
            @PathParam("id") String id,
            @Parameter(in=ParameterIn.QUERY, name = "sortBy", description = "Field name by which to sort", schema = @Schema(type = SchemaType.STRING)) //allowedValues = "termName,geneticEntity"
            @DefaultValue("termName") @QueryParam("sortBy") String sortBy,
            @Parameter(in=ParameterIn.QUERY, name = "filter.geneticEntity", description = "genetic entity symbol", schema = @Schema(type = SchemaType.STRING))
            @QueryParam("filter.geneticEntity") String geneticEntity,
            @Parameter(in=ParameterIn.QUERY, name = "filter.geneticEntityType", description = "genetic entity type", schema = @Schema(type = SchemaType.STRING))//allowedValues = "allele"
            @QueryParam("filter.geneticEntityType") String geneticEntityType,
            @Parameter(in=ParameterIn.QUERY, name = "filter.termName", description = "term name", schema = @Schema(type = SchemaType.STRING))
            @QueryParam("filter.termName") String phenotype,
            @Parameter(in=ParameterIn.QUERY, name = "filter.reference", description = "Reference number: PUBMED or a Pub ID from the MOD", schema = @Schema(type = SchemaType.STRING))
            @QueryParam("filter.reference") String reference,
            @Parameter(in=ParameterIn.QUERY, name = "asc", description = "order to sort by", schema = @Schema(type = SchemaType.STRING))
            @DefaultValue("true")
            @QueryParam("asc") String asc);

    @GET
    @Path("/{id}/models")
    @JsonView(value = {View.DiseaseAnnotationSummary.class})
    @Operation(summary = "Retrieve all DiseaseAnnotation records for a given disease id")
    JsonResultResponse<PrimaryAnnotatedEntity> getPrimaryAnnotatedEntityForModel(
            @Parameter(in=ParameterIn.PATH, name = "id", description = "gene ID: e.g. MGI:109583", required=true, schema = @Schema(type = SchemaType.STRING))
            @PathParam("id") String id,
            @Parameter(in=ParameterIn.QUERY, name = "limit", description = "Number of rows returned", schema = @Schema(type = SchemaType.INTEGER))
            @DefaultValue("20") @QueryParam("limit") int limit,
            @Parameter(in=ParameterIn.QUERY, name = "page", description = "Page number", schema = @Schema(type = SchemaType.INTEGER))
            @DefaultValue("1") @QueryParam("page") int page,
            @Parameter(in=ParameterIn.QUERY, name = "sortBy", description = "Field name by which to sort", schema = @Schema(type = SchemaType.STRING))// allowedValues = "Default,Gene,Disease,Species"
            @DefaultValue("geneName") @QueryParam("sortBy") String sortBy,
            @Parameter(in=ParameterIn.QUERY, name = "filter.modelName", description = "filter by model name", schema = @Schema(type = SchemaType.STRING))
            @QueryParam("filter.modelName") String modelName,
            @Parameter(in=ParameterIn.QUERY, name = "filter.species", description = "filter by species", schema = @Schema(type = SchemaType.STRING))
            @QueryParam("filter.species") String species,
            @Parameter(in=ParameterIn.QUERY, name = "filter.disease", description = "filter by disease", schema = @Schema(type = SchemaType.STRING))
            @QueryParam("filter.disease") String disease,
            @Parameter(in=ParameterIn.QUERY, name = "filter.phenotype", description = "filter by phenotype", schema = @Schema(type = SchemaType.STRING))
            @QueryParam("filter.phenotype") String phenotype,
            @Parameter(in=ParameterIn.QUERY, name = "filter.source", description = "filter by source", schema = @Schema(type = SchemaType.STRING))
            @QueryParam("filter.source") String source,
            @Parameter(in=ParameterIn.QUERY, name = "asc", description = "order to sort by", schema = @Schema(type = SchemaType.STRING))//,allowedValues = "true,false"
            @DefaultValue("true")
            @QueryParam("asc") String asc);




    @GET
    @Path("/{id}/homologs")
    @JsonView(value = {View.Orthology.class})
    @Operation(summary = "Download homology records.")
    JsonResultResponse<OrthologView> getGeneOrthology(
            @Parameter(in=ParameterIn.PATH, name = "id", description = "Source Gene ID: the gene for which you are searching homologous gene, e.g. 'MGI:109583'", required = true, schema = @Schema(type = SchemaType.STRING))
            @PathParam("id") String id,
            @Parameter(in=ParameterIn.QUERY, name = "geneId", description = "List of additional source gene IDs for which homology is retrieved." , schema = @Schema(type = SchemaType.STRING))
            @QueryParam("geneId") List<String> geneID,
            @Parameter(in=ParameterIn.QUERY, name = "geneList", description = "List of additional source gene IDs for which homology is retrieved in a comma-delimited list, e.g. 'MGI:109583,RGD:2129,MGI:97570'", schema = @Schema(type = SchemaType.STRING))
            @QueryParam("geneIdList") String geneList,
            @Parameter(in=ParameterIn.QUERY, name = "filter.stringency", description = "apply stringency containsFilterValue", schema = @Schema(type = SchemaType.STRING))
            @DefaultValue("stringent") @QueryParam("filter.stringency") String stringencyFilter,
            @Parameter(in=ParameterIn.QUERY, name = "taxonID", description = "Species identifier: Could be the full ID, e.g. 'NCBITaxon:10090', or just the ID, i.e. '10090'. Alternatively, part of a species name uniquely identifying a single species, e.g. 'danio' or 'mus'.", schema = @Schema(type = SchemaType.STRING))
            @QueryParam("filter.taxonID") String taxonID,
            @Parameter(in=ParameterIn.QUERY, name = "filter.method", description = "calculation methods", schema = @Schema(type = SchemaType.STRING))
            @QueryParam("filter.method") String method,
            @Parameter(in=ParameterIn.QUERY, name = "limit", description = "Number of rows returned", schema = @Schema(type = SchemaType.INTEGER))
            @DefaultValue("20") @QueryParam("limit") int limit,
            @Parameter(in=ParameterIn.QUERY, name = "page", description = "Page number", schema = @Schema(type = SchemaType.INTEGER))
            @DefaultValue("1") @QueryParam("page") int page) throws IOException;

    @GET
    @Path("/{id}/homologs-with-expression")
    @JsonView(value = {View.Orthology.class})
    @Operation(summary = "Retrieve homologous gene records that have expression data")
    JsonResultResponse<OrthologView> getGeneOrthologyWithExpression(
            @Parameter(in=ParameterIn.PATH, name = "id", description = "Source Gene ID: the gene for which you are searching homologous gene, e.g. 'MGI:109583'", required=true, schema = @Schema(type = SchemaType.STRING))
            @PathParam("id") String id,
            @Parameter(in=ParameterIn.QUERY, name = "stringencyFilter", description = "apply stringency containsFilterdescription", schema = @Schema(type = SchemaType.STRING))
            @DefaultValue("stringent") @QueryParam("stringencyFilter") String stringencyFilter);

    @GET
    @Path("/{id}/interactions")
    @Operation(summary = "Retrieve interactions for a given gene")
    @JsonView(value = {View.Interaction.class})
    JsonResultResponse<InteractionGeneJoin> getInteractions(
            @Parameter(in=ParameterIn.PATH, name = "id", description = " Gene ID", required=true, schema = @Schema(type = SchemaType.STRING))
            @PathParam("id") String id,
            @Parameter(in=ParameterIn.QUERY, name = "limit", description = "Number of rows returned", schema = @Schema(type = SchemaType.INTEGER))
            @DefaultValue("20") @QueryParam("limit") int limit,
            @Parameter(in=ParameterIn.QUERY, name = "page", description = "Page number", schema = @Schema(type = SchemaType.INTEGER))
            @DefaultValue("1") @QueryParam("page") int page,
            @Parameter(in=ParameterIn.QUERY, name = "sortBy", description = "Field name by which to sort", schema = @Schema(type = SchemaType.STRING))//, allowedValues = "nteractorGeneSymbol,interactorMoleculeType,interactorSpecies,interactorSpecies,reference")
            @QueryParam("sortBy") String sortBy,
            @Parameter(in=ParameterIn.QUERY, name = "asc", description = "order to sort by", schema = @Schema(type = SchemaType.STRING))//,allowedValues = "true,false")
            @DefaultValue("true")
            @QueryParam("asc") String asc,
            @Parameter(in=ParameterIn.QUERY,name = "filter.moleculeType")
            @QueryParam("filter.moleculeType") String moleculeType,
            @Parameter(in=ParameterIn.QUERY,name = "filter.interactorGeneSymbol" ,description = "Gene symbol")
            @QueryParam("filter.interactorGeneSymbol") String interactorGeneSymbol,
            @Parameter(in=ParameterIn.QUERY,name = "filter.interactorSpecies", description = "Species")
            @QueryParam("filter.interactorSpecies") String interactorSpecies,
            @Parameter(in=ParameterIn.QUERY,name = "filter.interactorMoleculeType", description = "molecule type")
            @QueryParam("filter.interactorMoleculeType") String interactorMoleculeType,
            @Parameter(in=ParameterIn.QUERY,name = "filter.detectionMethod", description = "detection method")
            @QueryParam("filter.detectionMethod") String detectionMethod,
            @Parameter(in=ParameterIn.QUERY,name = "filter.source", description = "database")
            @QueryParam("filter.source") String source,
            @Parameter(in=ParameterIn.QUERY,name = "filter.reference", description = "References")
            @QueryParam("filter.reference") String reference,
            @Context UriInfo info);

    @GET
    @Path("/{id}/interactions/download")
    @Operation(summary = "Retrieve interactions for a given gene")
    @Produces(MediaType.TEXT_PLAIN)
    Response getInteractionsDownload(
            @Parameter(in=ParameterIn.PATH, name = "id", description = " Gene ID", required=true, schema = @Schema(type = SchemaType.STRING))
            @PathParam("id") String id,
            @Parameter(in=ParameterIn.QUERY, name = "sortBy",description = "Name by which to sort", schema = @Schema(type = SchemaType.STRING))//allowedValues = "interactorGeneSymbol,interactorMoleculeType,interactorSpecies,interactorSpecies,reference")
            @QueryParam("sortBy") String sortBy,
            @Parameter(in=ParameterIn.QUERY, name = "asc", description= "ascending order: true or false", schema = @Schema(type = SchemaType.STRING))// allowedValues = "true,false")
            @DefaultValue("true")
            @QueryParam("asc") String asc,
            @Parameter(in=ParameterIn.QUERY, name = "filter.moleculeType", description = "molecule type")
            @QueryParam("filter.moleculeType") String moleculeType,
            @Parameter(in=ParameterIn.QUERY, name = "filter.interactorGeneSymbol", description = "gene symbol")
            @QueryParam("filter.interactorGeneSymbol") String interactorGeneSymbol,
            @Parameter(in=ParameterIn.QUERY, name = "filter.interactorSpecies", description = "species")
            @QueryParam("filter.interactorSpecies") String interactorSpecies,
            @Parameter(in=ParameterIn.QUERY, name = "filter.interactorMoleculeType", description = "molecule type")
            @QueryParam("filter.interactorMoleculeType") String interactorMoleculeType,
            @Parameter(in=ParameterIn.QUERY, name = "filter.detectionMethod", description = "detection method")
            @QueryParam("filter.detectionMethod") String detectionMethod,
            @Parameter(in=ParameterIn.QUERY, name = "filter.source", description = "database")
            @QueryParam("filter.source") String source,
            @Parameter(in=ParameterIn.QUERY, name = "filter.reference", description = "References")
            @QueryParam("filter.reference") String reference
    );

    @GET
    @Path("/{id}/expression-summary")
    @JsonView(value = {View.Expression.class})
    @Operation(summary = "Retrieve all expression records of a given gene")
    ExpressionSummary getExpressionSummary(
            @Parameter(in=ParameterIn.PATH, name = "id", description = "Gene by ID, e.g. 'RGD:2129' or 'ZFIN:ZDB-GENE-990415-72 fgf8a'", required=true, schema = @Schema(type = SchemaType.STRING))
            @PathParam("id") String id
    ) throws JsonProcessingException;

    @GET
    @Path("/{id}/disease-ribbon-summary")
    @JsonView(value = {View.DiseaseAnnotation.class})
    @Operation(summary = "Retrieve all disease records of a given gene")
    DiseaseRibbonSummary getDiseaseRibbonSummary(
            @Parameter(in=ParameterIn.PATH, name = "id", description = "Gene by ID, e.g. 'RGD:2129' or 'ZFIN:ZDB-GENE-990415-72 fgf8a'", required=true, schema = @Schema(type = SchemaType.STRING))
            @PathParam("id") String id,
            @Parameter(in=ParameterIn.QUERY, name = "geneID", description = "additional orthologous genes", required = true)
            @QueryParam("geneID") List<String> geneIDs
    ) throws JsonProcessingException;

    @GET
    @Path("/{id}/interaction-summary")
    @JsonView(value = {View.Expression.class})
    @Operation(summary = "Retrieve interaction summary records of a given gene")
    EntitySummary getInteractionSummary(
            @Parameter(in=ParameterIn.PATH, name = "id", description = "Gene by ID, e.g. 'RGD:2129' or 'ZFIN:ZDB-GENE-990415-72 fgf8a'", required=true, schema = @Schema(type = SchemaType.STRING))
            @PathParam("id") String id
    ) throws JsonProcessingException;


    @GET
    @Path("/{id}/diseases-by-experiment")
    @Operation(summary = "Retrieve disease annotations for a given gene")
    @JsonView(value = {View.DiseaseAnnotation.class})
    JsonResultResponse<DiseaseAnnotation> getDiseaseByExperiment(
            @Parameter(in=ParameterIn.PATH, name = "id", description = "Gene by ID, e.g. 'RGD:2129' or 'ZFIN:ZDB-GENE-990415-72 fgf8a'", required=true, schema = @Schema(type = SchemaType.STRING))
            @PathParam("id") String id,
            @Parameter(in=ParameterIn.QUERY, name = "limit", description = "Number of rows returned", schema = @Schema(type = SchemaType.INTEGER))
            @DefaultValue("20") @QueryParam("limit") int limit,
            @Parameter(in=ParameterIn.QUERY, name = "page", description = "Page number", schema = @Schema(type = SchemaType.INTEGER))
            @DefaultValue("1") @QueryParam("page") int page,
            @Parameter(in=ParameterIn.QUERY, name = "sortBy", description = "Field name by which to sort", schema = @Schema(type = SchemaType.STRING))//, allowedValues = "disease,geneticEntity")
            @DefaultValue("disease") @QueryParam("sortBy") String sortBy,
            @Parameter(in=ParameterIn.QUERY, name =  "geneticEntity", description = "genetic entity symbol", schema = @Schema(type = SchemaType.STRING))
            @QueryParam("filter.geneticEntity") String geneticEntity,
            @Parameter(in=ParameterIn.QUERY, name = "filter.geneticEntityType", description = "genetic entity type",schema = @Schema(type = SchemaType.STRING))// allowedValues = "allele,gene")
            @QueryParam("filter.geneticEntityType") String geneticEntityType,
            @Parameter(in=ParameterIn.QUERY, name = "filter.disease", description = "termName annotation")
            @QueryParam("filter.disease") String phenotype,
            @Parameter(in=ParameterIn.QUERY, name = "filter.associationType" ,description = "association type")
            @QueryParam("filter.associationType") String associationType,
            @Parameter(in=ParameterIn.QUERY, name = "filter.evidenceCode", description = "Evidence Code")
            @QueryParam("filter.evidenceCode") String evidenceCode,
            @Parameter(in=ParameterIn.QUERY, name = "source" , description = "Data Source")
            @QueryParam("source") String source,
            @Parameter(in=ParameterIn.QUERY, name ="publicaions", description = "Reference number: PUBMED or a Pub ID from the MOD")
            @QueryParam("publications") String reference,
            @Parameter(in=ParameterIn.QUERY, name = "asc", description = "order to sort by", schema = @Schema(type = SchemaType.STRING))//,allowedValues = "true,false")
            @DefaultValue("true")
            @QueryParam("asc") String asc,
            @Context UriInfo ui) throws JsonProcessingException;

    @GET
    @Path("/{id}/diseases-by-experiment/download")
    @Operation(summary = "Retrieve all disease annotations for a given gene and containsFilterdescription option", hidden = true)
    Response getDiseaseByExperimentDownload(
            @Parameter(in=ParameterIn.PATH, name = "id", description = "Gene by ID, e.g. 'RGD:2129' or 'ZFIN:ZDB-GENE-990415-72 fgf8a'", required=true, schema = @Schema(type = SchemaType.STRING))
            @PathParam("id") String id,
            @DefaultValue("disease") @QueryParam("sortBy") String sortBy,
            @Parameter(in=ParameterIn.QUERY, name =  "geneticEntity", description = "genetic entity symbol",schema = @Schema(type = SchemaType.STRING))
            @QueryParam("filter.geneticEntity") String geneticEntity,
            @Parameter(in=ParameterIn.QUERY, name = "filter.geneticEntityType", description = "genetic entity type",schema = @Schema(type = SchemaType.STRING))// allowedValues = "allele,gene")
            @QueryParam("filter.geneticEntityType") String geneticEntityType,
            @Parameter(in=ParameterIn.QUERY, name = "filter.disease", description = "termName annotation")
            @QueryParam("filter.disease") String phenotype,
            @Parameter(in=ParameterIn.QUERY, name = "filter.associationType" ,description = "association type")
            @QueryParam("filter.associationType") String associationType,
            @Parameter(in=ParameterIn.QUERY, name = "filter.evidenceCode", description = "Evidence Code")
            @QueryParam("filter.evidenceCode") String evidenceCode,
            @Parameter(in=ParameterIn.QUERY, name = "source" , description = "Data Source")
            @QueryParam("source") String source,
            @Parameter(in=ParameterIn.QUERY, name ="publicaions", description = "Reference number: PUBMED or a Pub ID from the MOD")
            @QueryParam("publications") String reference,
            @Parameter(in=ParameterIn.QUERY, name = "asc", description = "order to sort by", schema = @Schema(type = SchemaType.STRING))//,allowedValues = "true,false")
            @DefaultValue("true")
            @QueryParam("asc") String asc,
            @Context UriInfo ui) throws JsonProcessingException;

    @GET
    @Path("/{id}/disease-summary")
    @Operation(summary = "Retrieve disease summary info for a given gene and disease type")
    DiseaseSummary getDiseaseSummary(
            @Parameter(in=ParameterIn.PATH, name = "id", description = "Gene by ID, e.g. 'RGD:2129' or 'ZFIN:ZDB-GENE-990415-72 fgf8a'", required=true, schema = @Schema(type = SchemaType.STRING))
            @PathParam("id") String id,
            @Parameter(in=ParameterIn.QUERY, name = "type",schema = @Schema(type = SchemaType.STRING))// allowedValues = "experiment,orthology")
            @DefaultValue("experiment")
            @QueryParam("type") String type
    ) throws JsonProcessingException;

    @GET
    @Path("/{id}/phenotype-summary")
    @Operation(summary = "Retrieve phenotype summary info for a given gene")
    EntitySummary getPhenotypeSummary(
            @Parameter(in=ParameterIn.PATH, name = "id", description = "Gene by ID, e.g. 'RGD:2129' or 'ZFIN:ZDB-GENE-990415-72 fgf8a'", required=true, schema = @Schema(type = SchemaType.STRING))
            @PathParam("id") String id
    ) throws JsonProcessingException;
}
