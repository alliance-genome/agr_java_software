package org.alliancegenome.api.rest.interfaces;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.alliancegenome.core.service.JsonResultResponse;
import org.alliancegenome.neo4j.entity.DiseaseAnnotation;
import org.alliancegenome.neo4j.entity.node.DOTerm;
import org.alliancegenome.neo4j.view.View;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.ParameterIn;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.core.JsonProcessingException;

@Path("/disease")
@Tag(name = "Disease ")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface DiseaseRESTInterface {

    @GET
    @Path("/{id}")
    @JsonView(value = {View.DiseaseAPI.class})
    @Operation(summary = "Retrieve a Disease object for a given id")

    public DOTerm getDisease(
            @Parameter(in = ParameterIn.PATH, name = "id", description = "Search for a Disease by ID", required = true, schema = @Schema(type = SchemaType.STRING))
            @PathParam("id") String id
    );

    @GET
    @Path("/{id}/associations")
    @JsonView(value = {View.DiseaseAnnotationSummary.class})
    @Operation(summary = "Retrieve all DiseaseAnnotation records for a given disease id" , hidden = true)
    JsonResultResponse<DiseaseAnnotation> getDiseaseAnnotationsSorted(
            @Parameter(in=ParameterIn.PATH, name="id", description = "Search for a disease by ID", required=true, schema = @Schema(type = SchemaType.STRING))
            @PathParam("id") String id,
            @Parameter(in=ParameterIn.QUERY, name="limit", description = "Number of rows returned", schema = @Schema(type = SchemaType.INTEGER))
            @DefaultValue("20") @QueryParam("limit") int limit,
            @Parameter(in=ParameterIn.QUERY, name="page", description = "Page number", schema = @Schema(type = SchemaType.INTEGER))
            @DefaultValue("1") @QueryParam("page") int page,
            @Parameter(in=ParameterIn.QUERY, name="sortBy", description = "Field name by which to sort", schema = @Schema(type = SchemaType.STRING))//, allowedValues = "Default,Gene,Disease,Species")
            @DefaultValue("geneName") @QueryParam("sortBy") String sortBy,
            @Parameter(in=ParameterIn.QUERY, name="filter.geneName", description = "filter by gene symbol")
            @QueryParam("filter.geneName") String geneName,
            @Parameter(in=ParameterIn.QUERY, name="filter.species", description = "filter by species")
            @QueryParam("filter.species") String species,
            @Parameter(in=ParameterIn.QUERY, name="filter.geneticEntity", description = "filter by gene genetic Entity")
            @QueryParam("filter.geneticEntity") String geneticEntity,
            @Parameter(in=ParameterIn.QUERY, name="filter.geneticEntityType" , description = "filter by genetic Entity type")//, allowedValues = "gene,allele")
            @QueryParam("filter.geneticEntityType") String geneticEntityType,
            @Parameter(in=ParameterIn.QUERY, name="filter.disease", description = "filter by disease")
            @QueryParam("filter.disease") String disease,
            @Parameter(in=ParameterIn.QUERY, name="filter.source" , description = "filter by source")
            @QueryParam("filter.source") String source,
            @Parameter(in=ParameterIn.QUERY, name="filter.reference", description = "filter by reference")
            @QueryParam("filter.reference") String reference,
            @Parameter(in=ParameterIn.QUERY, name="filter.evidenceCode", description = "filter by evidence code")
            @QueryParam("filter.evidenceCode") String evidenceCode,
            @Parameter(in=ParameterIn.QUERY, name="filter.basedOnGeneSymbol", description = "filter by based-on-gene")
            @QueryParam("filter.basedOnGeneSymbol") String basedOnGeneSymbol,
            @Parameter(in=ParameterIn.QUERY, name="filter.associationType", description = "filter by association type")
            @QueryParam("filter.associationType") String associationType,
            @Parameter(in=ParameterIn.QUERY, name = "asc", description = "order to sort by", schema = @Schema(type = SchemaType.STRING))//,allowedValues = "true,false")
            @DefaultValue("true")
            @QueryParam("asc") String asc);

    @GET
    @Path("/{id}/alleles")
    @JsonView(value = {View.DiseaseAnnotationSummary.class})
    @Operation(summary = "Retrieve all DiseaseAnnotation records for a given disease id")
    JsonResultResponse<DiseaseAnnotation> getDiseaseAnnotationsByAllele(
            //@ApiParam(name = "id", value = "Disease by DOID: e.g. DOID:9952", required = true, type = "String")
            @PathParam("id") String id,
            //@ApiParam(name = "limit", value = "Number of rows returned", defaultValue = "20")
            @DefaultValue("20") @QueryParam("limit") Integer limit,
            //@ApiParam(name = "page", value = "Page number")
            @DefaultValue("1") @QueryParam("page") Integer page,
            //@ApiParam(value = "Field / column name by which to sort", allowableValues = "Default,Allele,Disease,Species", defaultValue = "Allele")
            @DefaultValue("DiseaseAlleleDefault") @QueryParam("sortBy") String sortBy,
            //@ApiParam(value = "filter by gene symbol")
            @QueryParam("filter.geneName") String geneName,
            //@ApiParam(value = "filter by allele symbol")
            @QueryParam("filter.alleleName") String alleleName,
            //@ApiParam(value = "filter by species")
            @QueryParam("filter.species") String species,
            //@ApiParam(value = "filter by disease")
            @QueryParam("filter.disease") String disease,
            //@ApiParam(value = "filter by source")
            @QueryParam("filter.source") String source,
            //@ApiParam(value = "filter by reference")
            @QueryParam("filter.reference") String reference,
            //@ApiParam(value = "filter by evidence code")
            @QueryParam("filter.evidenceCode") String evidenceCode,
            //@ApiParam(value = "filter by association type")
            @QueryParam("filter.associationType") String associationType,
            //@ApiParam(value = "ascending order: true or false", allowableValues = "true,false", defaultValue = "true")
            @QueryParam("asc") String asc);

    @GET
    @Path("/{id}/alleles/download")
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(summary = "downlaod all DiseaseAnnotation records for a given allele id")
    Response getDiseaseAnnotationsByAlleleDownload(
            @Parameter(in=ParameterIn.PATH, name="id", description = "Search for a allele by ID", required=true, schema = @Schema(type = SchemaType.STRING))
            @PathParam("id") String id,
            @DefaultValue("Allele") @QueryParam("sortBy") String sortBy,
            @Parameter(in=ParameterIn.QUERY, name ="filter.geneName",description = "filter by gene symbol")
            @QueryParam("filter.geneName") String geneName,
            @Parameter(in=ParameterIn.QUERY, name ="filter.alleleName",description = "filter by allele symbol")
            @QueryParam("filter.alleleName") String alleleName,
            @Parameter(in=ParameterIn.QUERY, name ="filter.species",description = "filter by species")
            @QueryParam("filter.species") String species,
            @Parameter(in=ParameterIn.QUERY, name ="filter.disease",description = "filter by disease")
            @QueryParam("filter.disease") String disease,
            @Parameter(in=ParameterIn.QUERY, name ="filter.source", description = "filter by source")
            @QueryParam("filter.source") String source,
            @Parameter(in=ParameterIn.QUERY, name ="filter.reference", description = "filter by reference")
            @QueryParam("filter.reference") String reference,
            @Parameter(in=ParameterIn.QUERY, name ="filter.evidenceCode", description = "filter by evidence code")
            @QueryParam("filter.evidenceCode") String evidenceCode,
            @Parameter(in=ParameterIn.QUERY, name ="filter.associationType",description = "filter by association type")
            @QueryParam("filter.associationType") String associationType,
            @Parameter(in=ParameterIn.QUERY, name = "asc", description = "order to sort by", schema = @Schema(type = SchemaType.STRING))//,allowedValues = "true,false")
            @DefaultValue("true")
            @QueryParam("asc") String asc);




    @GET
    @Path("/{id}/genes")
    @JsonView(value = {View.DiseaseAnnotationSummary.class})
    @Operation(summary = "Retrieve all DiseaseAnnotation records for a given disease id")
    JsonResultResponse<DiseaseAnnotation> getDiseaseAnnotationsByGene(
            @Parameter(in=ParameterIn.PATH, name="id", description = "Search for a disease by ID", required=true, schema = @Schema(type = SchemaType.STRING))
            @PathParam("id") String id,
            @Parameter(in=ParameterIn.QUERY, name="limit", description = "Number of rows returned", schema = @Schema(type = SchemaType.INTEGER))
            @DefaultValue("20") @QueryParam("limit") Integer limit,
            @Parameter(in=ParameterIn.QUERY, name="page", description = "Page number", schema = @Schema(type = SchemaType.INTEGER))
            @DefaultValue("1") @QueryParam("page") Integer page,
            @Parameter(in=ParameterIn.QUERY, name = "sortBy", description = "order to sort by", schema = @Schema(type = SchemaType.STRING))
            @QueryParam("sortBy") String sortBy,
           @Parameter(in=ParameterIn.QUERY, name ="filter.geneName",description = "filter by gene symbol")
            @QueryParam("filter.geneName") String geneSymbol,
            @Parameter(in=ParameterIn.QUERY, name ="filter.species",description = "filter by species")
            @QueryParam("filter.species") String species,
            @Parameter(in=ParameterIn.QUERY, name ="filter.disease",description = "filter by disease")
            @QueryParam("filter.disease") String disease,
            @Parameter(in=ParameterIn.QUERY, name ="filter.source", description = "filter by source")
            @QueryParam("filter.source") String source,
            @Parameter(in=ParameterIn.QUERY, name ="filter.reference", description = "filter by reference")
            @QueryParam("filter.reference") String reference,
            @Parameter(in=ParameterIn.QUERY, name ="filter.evidenceCode", description = "filter by evidence code")
            @QueryParam("filter.evidenceCode") String evidenceCode,
            @Parameter(in=ParameterIn.QUERY, name ="filter.associationType",description = "filter by association type")
            @QueryParam("filter.associationType") String associationType,
            @Parameter(in=ParameterIn.QUERY, name = "asc", description = "order to sort by", schema = @Schema(type = SchemaType.STRING))
            @QueryParam("asc") String asc);



    @GET
    @Path("/{id}/genes/download")
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(summary = "Retrieve all DiseaseAnnotation records for a given disease id")
    Response getDiseaseAnnotationsByGeneDownload(
            @Parameter(in=ParameterIn.PATH, name="id", description = "Search for a disease by ID", required=true, schema = @Schema(type = SchemaType.STRING))
            @PathParam("id") String id,
            @Parameter(in=ParameterIn.QUERY, name = "sortBy", description = "order to sort by", schema = @Schema(type = SchemaType.STRING))//, allowedValues = "Default,Gene,Disease,Species")
            @DefaultValue("Gene") @QueryParam("sortBy") String sortBy,
            @Parameter(in=ParameterIn.QUERY, name ="filter.geneName",description = "filter by gene symbol")
            @QueryParam("filter.geneName") String geneName,
            @Parameter(in=ParameterIn.QUERY, name ="filter.species",description = "filter by species")
            @QueryParam("filter.species") String species,
            @Parameter(in=ParameterIn.QUERY, name ="filter.disease",description = "filter by disease")
            @QueryParam("filter.disease") String disease,
            @Parameter(in=ParameterIn.QUERY, name ="filter.source", description = "filter by source")
            @QueryParam("filter.source") String source,
            @Parameter(in=ParameterIn.QUERY, name ="filter.reference", description = "filter by reference")
            @QueryParam("filter.reference") String reference,
            @Parameter(in=ParameterIn.QUERY, name ="filter.evidenceCode", description = "filter by evidence code")
            @QueryParam("filter.evidenceCode") String evidenceCode,
            @Parameter(in=ParameterIn.QUERY, name ="filter.associationType",description = "filter by association type")
            @QueryParam("filter.associationType") String associationType,
            @Parameter(in=ParameterIn.QUERY, name = "asc", description = "order to sort by", schema = @Schema(type = SchemaType.STRING))//,allowedValues = "true,false")
            @DefaultValue("true")
            @QueryParam("asc") String asc);



    @GET
    @Path("/{id}/models")
    @JsonView(value = {View.DiseaseAnnotationSummary.class})
    @Operation(summary = "Retrieve all DiseaseAnnotation records for a given disease id")
    JsonResultResponse<DiseaseAnnotation> getDiseaseAnnotationsForModel(
            //@ApiParam(name = "id", value = "Disease by DOID: e.g. DOID:9952", required = true, type = "String")
            @PathParam("id") String id,
            //@ApiParam(name = "limit", value = "Number of rows returned", defaultValue = "20")
            @DefaultValue("20") @QueryParam("limit") Integer limit,
            //@ApiParam(name = "page", value = "Page number")
            @DefaultValue("1") @QueryParam("page") Integer page,
            //@ApiParam(value = "Field / column name by which to sort", allowableValues = "Default,Gene,Disease,Species", defaultValue = "geneName")
            @QueryParam("sortBy") String sortBy,
            //@ApiParam(value = "filter by model name")
            @QueryParam("filter.modelName") String modelName,
            //@ApiParam(value = "filter by gene symbol")
            @QueryParam("filter.geneName") String geneSymbol,
            //@ApiParam(value = "filter by species")
            @QueryParam("filter.species") String species,
            //@ApiParam(value = "filter by disease")
            @QueryParam("filter.disease") String disease,
            //@ApiParam(value = "filter by source")
            @QueryParam("filter.source") String source,
            //@ApiParam(value = "filter by reference")
            @QueryParam("filter.reference") String reference,
            //@ApiParam(value = "filter by evidence code")
            @QueryParam("filter.evidenceCode") String evidenceCode,
            //@ApiParam(value = "ascending order: true or false", allowableValues = "true,false", defaultValue = "true")
            @QueryParam("asc") String asc);




   /* @GET
    @Path("/{id}/models")
    @JsonView(value = {View.DiseaseAnnotationSummary.class})
    @Operation(summary = "Retrieve all DiseaseAnnotation records for a given disease id")
    JsonResultResponse<DiseaseAnnotation> getDiseaseAnnotationsForModel(
            @Parameter(in=ParameterIn.PATH, name="id", description = "Search for a disease by ID", required=true, schema = @Schema(type = SchemaType.STRING))
            @PathParam("id") String id,
            @Parameter(in=ParameterIn.QUERY, name="limit", description = "Number of rows returned", schema = @Schema(type = SchemaType.INTEGER))
            @DefaultValue("20") @QueryParam("limit") int limit,
            @Parameter(in=ParameterIn.QUERY, name="page", description = "Page number", schema = @Schema(type = SchemaType.INTEGER))
            @DefaultValue("1") @QueryParam("page") int page,
            @Parameter(in=ParameterIn.QUERY, name = "sortBy", description = "order to sort by", schema = @Schema(type = SchemaType.STRING))//, allowedValues = "Default,Gene,Disease,Species")
            @DefaultValue("Gene") @QueryParam("sortBy") String sortBy,
            @Parameter(in=ParameterIn.QUERY, name ="filter.modelName", description = "filter by model name")
            @QueryParam("filter.modelName") String modelName,
            @Parameter(in=ParameterIn.QUERY, name ="filter.geneName",description = "filter by gene symbol")
            @QueryParam("filter.geneName") String geneName,
            @Parameter(in=ParameterIn.QUERY, name ="filter.species",description = "filter by species")
            @QueryParam("filter.species") String species,
            @Parameter(in=ParameterIn.QUERY, name ="filter.disease", description = "filter by disease")
            @QueryParam("filter.disease") String disease,
            @Parameter(in=ParameterIn.QUERY, name ="filter.source", description = "filter by source")
            @QueryParam("filter.source") String source,
            @Parameter(in=ParameterIn.QUERY, name ="filter.reference", description = "filter by reference")
            @QueryParam("filter.reference") String reference,
            @Parameter(in=ParameterIn.QUERY, name ="filter.evidenceCode", description = "filter by evidence code")
            @QueryParam("filter.evidenceCode") String evidenceCode,
            @Parameter(in=ParameterIn.QUERY, name = "asc", description = "order to sort by", schema = @Schema(type = SchemaType.STRING))//,allowedValues = "true,false")
    @DefaultValue("true")
    @QueryParam("asc") String asc);*/

    @GET
    @Path("/{id}/models/download")
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(summary = "Retrieve all DiseaseAnnotation records for a given disease id")
    Response getDiseaseAnnotationsForModelDownload(
            @Parameter(in=ParameterIn.PATH, name="id", description = "Search for a disease by ID", required=true, schema = @Schema(type = SchemaType.STRING))
            @PathParam("id") String id,
            @Parameter(in=ParameterIn.QUERY, name = "sortBy", description = "order to sort by", schema = @Schema(type = SchemaType.STRING))// allowedValues = "Default,Gene,Disease,Species")
            @DefaultValue("Gene") @QueryParam("sortBy") String sortBy,
            @Parameter(in=ParameterIn.QUERY, name ="filter.modelName", description = "filter by model name")
            @QueryParam("filter.modelName") String modelName,
            @Parameter(in=ParameterIn.QUERY, name ="filter.geneName",description = "filter by gene symbol")
            @QueryParam("filter.geneName") String geneSymbol,
            @Parameter(in=ParameterIn.QUERY, name ="filter.species",description = "filter by species")
            @QueryParam("filter.species") String species,
            @Parameter(in=ParameterIn.QUERY, name ="filter.disease", description = "filter by disease")
            @QueryParam("filter.disease") String disease,
            @Parameter(in=ParameterIn.QUERY, name ="filter.source", description = "filter by source")
            @QueryParam("filter.source") String source,
            @Parameter(in=ParameterIn.QUERY, name ="filter.reference", description = "filter by reference")
            @QueryParam("filter.reference") String reference,
            @Parameter(in=ParameterIn.QUERY, name ="filter.evidenceCode", description = "filter by evidence code")
            @QueryParam("filter.evidenceCode") String evidenceCode,
            @Parameter(in=ParameterIn.QUERY, name = "asc", description = "order to sort by", schema = @Schema(type = SchemaType.STRING))//,allowedValues = "true,false")
    @DefaultValue("true")
    @QueryParam("asc") String asc);


    
    @GET
    @Path("/{id}/associations/download")
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(summary = "Download all DiseaseAnnotation records for a given disease id and sorting / filtering parameters" , hidden = true)
    Response getDiseaseAnnotationsDownloadFile(
            @Parameter(in=ParameterIn.PATH, name="id", description= "Disease by DOID: e.g. DOID:9952", required = true, schema = @Schema(type = SchemaType.STRING))
            @PathParam("id") String id,
            @Parameter(in=ParameterIn.QUERY, name = "sortBy", description = "order to sort by", schema = @Schema(type = SchemaType.STRING))//, allowedValues = "Default,Gene,Disease,Species")
            @DefaultValue("Gene") @QueryParam("sortBy") String sortBy,
            @Parameter(in=ParameterIn.QUERY, name ="filter.geneName",description = "filter by gene symbol")
            @QueryParam("filter.geneName") String geneName,
            @Parameter(in=ParameterIn.QUERY, name ="filter.species", description="filter by species")
            @QueryParam("filter.species") String species,
            @Parameter(in=ParameterIn.QUERY, name ="geneticEntity", description = "filter by gene genetic Entity")
            @QueryParam("filter.geneticEntity") String geneticEntity,
            @Parameter(in=ParameterIn.QUERY, name ="filter.geneticEntityType" ,description = "filter by genetic Entity type",schema = @Schema(type = SchemaType.STRING))//, allowedValues = "gene,allele")
            @QueryParam("filter.geneticEntityType") String geneticEntityType,
            @Parameter(in=ParameterIn.QUERY, name ="filter.disease", description = "filter by disease")
            @QueryParam("filter.disease") String disease,
            @Parameter(in=ParameterIn.QUERY, name ="filter.source",description = "filter by source")
            @QueryParam("filter.source") String source,
            @Parameter(in=ParameterIn.QUERY, name ="filter.reference",description = "filter by reference")
            @QueryParam("filter.reference") String reference,
            @Parameter(in=ParameterIn.QUERY, name ="filter.evidenceCode",description = "filter by evidence code")
            @QueryParam("filter.evidenceCode") String evidenceCode,
            @Parameter(in=ParameterIn.QUERY, name ="filter.basedOnGeneSymbol",description = "filter by based-on-gene")
            @QueryParam("filter.basedOnGeneSymbol") String basedOnGeneSymbol,
            @Parameter(in=ParameterIn.QUERY, name ="filter.associationType",description = "filter by association type")
            @QueryParam("filter.associationType") String associationType,
            @Parameter(in=ParameterIn.QUERY, name = "asc", description = "order to sort by", schema = @Schema(type = SchemaType.STRING))//,allowedValues = "true,false")
            @DefaultValue("true")
            @QueryParam("asc") String asc);

    @GET
    @Path("/{id}/associations/download/all")
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(description = "Retrieve all DiseaseAnnotation records for a given disease id disregarding sorting / filtering parameters" , hidden = true)
    String getDiseaseAnnotationsDownload(@PathParam("id") String id);

    @GET
    @Path("")
    @JsonView(value = {View.DiseaseAnnotation.class})
    @Operation(summary = "Retrieve all disease annotations of a given set of genes")
    JsonResultResponse<DiseaseAnnotation> getDiseaseAnnotationsRibbonDetails(
            @Parameter(in=ParameterIn.PATH, name = "geneID", description = "Gene by ID", required = true)
            @QueryParam("geneID") List<String> geneIDs,
            @Parameter(in=ParameterIn.PATH, name = "termID", description = "Term ID by which rollup should happen")
            @QueryParam("termID") String termID,
            @Parameter(in=ParameterIn.PATH,name = "filter.species", description = "Species by taxon ID", schema = @Schema(type = SchemaType.STRING))
            @QueryParam("filter.species") String filterSpecies,
            @Parameter(in=ParameterIn.PATH, name = "filter.gene", description = "Gene symbol", schema = @Schema(type = SchemaType.STRING))
            @QueryParam("filter.gene") String filterGene,
            @Parameter(in=ParameterIn.PATH,name = "filter.reference", description = "Reference", schema = @Schema(type = SchemaType.STRING))
            @QueryParam("filter.reference") String filterReference,
            @Parameter(in=ParameterIn.PATH,name = "filter.disease", description = "Ontology term name", schema = @Schema(type = SchemaType.STRING))
            @QueryParam("filter.disease") String diseaseTerm,
            @Parameter(in=ParameterIn.PATH,name = "filter.source", description = "Source", schema = @Schema(type = SchemaType.STRING))
            @QueryParam("filter.source") String filterSource,
            @Parameter(in=ParameterIn.PATH,name = "filter.geneticEntity", description = "geneticEntity", schema = @Schema(type = SchemaType.STRING))
            @QueryParam("filter.geneticEntity") String geneticEntity,
            @Parameter(in=ParameterIn.PATH,name = "filter.geneticEntityType", description = "geneticEntityType", schema = @Schema(type = SchemaType.STRING))
            @QueryParam("filter.geneticEntityType") String geneticEntityType,
            @Parameter(in=ParameterIn.PATH,name = "filter.associationType", description = "associationType", schema = @Schema(type = SchemaType.STRING))
            @QueryParam("filter.associationType") String associationType,
            @Parameter(in=ParameterIn.PATH,name = "filter.evidenceCode", description = "evidenceCode", schema = @Schema(type = SchemaType.STRING))
            @QueryParam("filter.evidenceCode") String evidenceCode,
            @Parameter(in=ParameterIn.PATH,name = "filter.basedOnGeneSymbol", description = "basedOnGeneSymbol", schema = @Schema(type = SchemaType.STRING))
            @QueryParam("filter.basedOnGeneSymbol") String basedOnGeneSymbol,
            @Parameter(in=ParameterIn.PATH,name = "limit", description = "Number of rows returned",schema = @Schema(type = SchemaType.INTEGER))
            @DefaultValue("20") @QueryParam("limit") int limit,
            @Parameter(in=ParameterIn.PATH,name = "page", description = "Page number")
            @DefaultValue("1") @QueryParam("page") int page,
            @Parameter(in=ParameterIn.PATH,name = "sortBy", description = "Sort by field name")
            @QueryParam("sortBy") String sortBy,
            @Parameter(in=ParameterIn.PATH,name = "asc",  description = "ascending or descending",schema = @Schema(type = SchemaType.STRING))//allowedValues = "true,false",
            @DefaultValue("true") @QueryParam("asc") String asc
    ) throws JsonProcessingException;

    @GET
    @Path("/download")
    @JsonView(value = {View.DiseaseAnnotation.class})
    @Operation(summary = "Download all disease annotations of a given set of genes")
    Response getDiseaseAnnotationsRibbonDetailsDownload(
            @Parameter(in=ParameterIn.PATH, name = "geneID", description = "Gene by ID", required = true)
            @QueryParam("geneID") List<String> geneIDs,
            @Parameter(in=ParameterIn.PATH, name = "termID", description = "Term ID by which rollup should happen")
            @QueryParam("termID") String termID,
            @Parameter(in=ParameterIn.PATH,name = "filter.species", description = "Species by taxon ID",schema = @Schema(type = SchemaType.STRING))
            @QueryParam("filter.species") String filterSpecies,
            @Parameter(in=ParameterIn.PATH, name = "filter.gene", description = "Gene symbol",schema = @Schema(type = SchemaType.STRING))
            @QueryParam("filter.gene") String filterGene,
            @Parameter(in=ParameterIn.PATH,name = "filter.reference", description = "Reference",schema = @Schema(type = SchemaType.STRING))
            @QueryParam("filter.reference") String filterReference,
            @Parameter(in=ParameterIn.PATH,name = "filter.disease", description = "Ontology term name",schema = @Schema(type = SchemaType.STRING))
            @QueryParam("filter.disease") String diseaseTerm,
            @Parameter(in=ParameterIn.PATH,name = "filter.source", description = "Source",schema = @Schema(type = SchemaType.STRING))
            @QueryParam("filter.source") String filterSource,
            @Parameter(in=ParameterIn.PATH,name = "filter.geneticEntity", description = "geneticEntity",schema = @Schema(type = SchemaType.STRING))
            @QueryParam("filter.geneticEntity") String geneticEntity,
            @Parameter(in=ParameterIn.PATH,name = "filter.geneticEntityType", description = "geneticEntityType",schema = @Schema(type = SchemaType.STRING))
            @QueryParam("filter.geneticEntityType") String geneticEntityType,
            @Parameter(in=ParameterIn.PATH,name = "filter.associationType", description = "associationType",schema = @Schema(type = SchemaType.STRING))
            @QueryParam("filter.associationType") String associationType,
            @Parameter(in=ParameterIn.PATH,name = "filter.evidenceCode", description = "evidenceCode",schema = @Schema(type = SchemaType.STRING))
            @QueryParam("filter.evidenceCode") String evidenceCode,
            @Parameter(in=ParameterIn.PATH,name = "filter.basedOnGeneSymbol", description = "basedOnGeneSymbol",schema = @Schema(type = SchemaType.STRING))
            @QueryParam("filter.basedOnGeneSymbol") String basedOnGeneSymbol,
            @Parameter(in=ParameterIn.PATH,name = "sortBy", description = "Sort by field name")
            @QueryParam("sortBy") String sortBy,
            @Parameter(in=ParameterIn.PATH,name = "asc",  description = "ascending or descending",schema = @Schema(type = SchemaType.STRING))//allowedValues = "true,false"
            @DefaultValue("true") @QueryParam("asc") String asc
    ) throws JsonProcessingException;

}
