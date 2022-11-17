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

import org.alliancegenome.cache.repository.helper.JsonResultResponse;
import org.alliancegenome.neo4j.entity.DiseaseAnnotation;
import org.alliancegenome.neo4j.entity.node.DOTerm;
import org.alliancegenome.neo4j.view.View;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.ParameterIn;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jose4j.json.internal.json_simple.JSONObject;

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
	@APIResponses(
			value = {
					@APIResponse(
							responseCode = "404",
							description = "Missing Disease object",
							content = @Content(mediaType = "text/plain")),
					@APIResponse(
							responseCode = "200",
							description = "Disease object.",
							content = @Content(mediaType = "application/json",
									schema = @Schema(implementation = DOTerm.class)))})

	public DOTerm getDisease(
			@Parameter(in = ParameterIn.PATH, name = "id", description = "Search for a Disease by ID", required = true, schema = @Schema(type = SchemaType.STRING))
			@PathParam("id") String id
	);

	@GET
	@Path("/{id}/associations")
	@JsonView(value = {View.DiseaseAnnotationSummary.class})
	@Operation(summary = "Retrieve all DiseaseAnnotation records for a given disease id", hidden = true)
	@APIResponses(
			value = {
					@APIResponse(
							responseCode = "404",
							description = "Missing disease annotations",
							content = @Content(mediaType = "text/plain")),
					@APIResponse(
							responseCode = "200",
							description = "Disease Annotations for a disease id.",
							content = @Content(mediaType = "application/json",
									schema = @Schema(implementation = View.DiseaseAnnotationSummary.class)))})
	JsonResultResponse<DiseaseAnnotation> getDiseaseAnnotationsSorted(
			@Parameter(in = ParameterIn.PATH, name = "id", description = "Search for a disease by ID", required = true, schema = @Schema(type = SchemaType.STRING))
			@PathParam("id") String id,
			@Parameter(in = ParameterIn.QUERY, name = "limit", description = "Number of rows returned", schema = @Schema(type = SchemaType.INTEGER))
			@DefaultValue("20") @QueryParam("limit") Integer limit,
			@Parameter(in = ParameterIn.QUERY, name = "page", description = "Page number", schema = @Schema(type = SchemaType.INTEGER))
			@DefaultValue("1") @QueryParam("page") Integer page,
			@Parameter(in = ParameterIn.QUERY, name = "sortBy", description = "Field name by which to sort", schema = @Schema(type = SchemaType.STRING))
//, allowedValues = "Default,Gene,Disease,Species")
			@DefaultValue("geneName") @QueryParam("sortBy") String sortBy,
			@Parameter(in = ParameterIn.QUERY, name = "filter.geneName", description = "filter by gene symbol")
			@QueryParam("filter.geneName") String geneName,
			@Parameter(in = ParameterIn.QUERY, name = "filter.species", description = "filter by species")
			@QueryParam("filter.species") String species,
			@Parameter(in = ParameterIn.QUERY, name = "filter.geneticEntity", description = "filter by gene genetic Entity")
			@QueryParam("filter.geneticEntity") String geneticEntity,
			@Parameter(in = ParameterIn.QUERY, name = "filter.geneticEntityType", description = "filter by genetic Entity type")
//, allowedValues = "gene,allele")
			@QueryParam("filter.geneticEntityType") String geneticEntityType,
			@Parameter(in = ParameterIn.QUERY, name = "filter.disease", description = "filter by disease")
			@QueryParam("filter.disease") String disease,
			@Parameter(in = ParameterIn.QUERY, name = "filter.source", description = "filter by source")
			@QueryParam("filter.source") String source,
			@Parameter(in = ParameterIn.QUERY, name = "filter.reference", description = "filter by reference")
			@QueryParam("filter.reference") String reference,
			@Parameter(in = ParameterIn.QUERY, name = "filter.evidenceCode", description = "filter by evidence code")
			@QueryParam("filter.evidenceCode") String evidenceCode,
			@Parameter(in = ParameterIn.QUERY, name = "filter.basedOnGeneSymbol", description = "filter by based-on-gene")
			@QueryParam("filter.basedOnGeneSymbol") String basedOnGeneSymbol,
			@Parameter(in = ParameterIn.QUERY, name = "filter.associationType", description = "filter by association type")
			@QueryParam("filter.associationType") String associationType,
			@Parameter(in = ParameterIn.QUERY, name = "asc", description = "order to sort by", schema = @Schema(type = SchemaType.STRING))
//,allowedValues = "true,false")
			@DefaultValue("true")
			@QueryParam("asc") String asc);

	@GET
	@Path("/{id}/alleles")
	@JsonView(value = {View.DiseaseAnnotationSummary.class})
	@Operation(summary = "Retrieve all DiseaseAnnotation records for a given disease id")
	JsonResultResponse<DiseaseAnnotation> getDiseaseAnnotationsByAllele(
			@Parameter(in = ParameterIn.PATH, name = "id", description = "Disease by DOID: e.g. DOID:9952", required = true, schema = @Schema(type = SchemaType.STRING))
			@PathParam("id") String id,
			@Parameter(in = ParameterIn.QUERY, name = "limit", description = "Number of rows returned", schema = @Schema(type = SchemaType.INTEGER))
			@DefaultValue("20") @QueryParam("limit") Integer limit,
			@Parameter(in = ParameterIn.QUERY, name = "page", description = "Page number", schema = @Schema(type = SchemaType.INTEGER))
			@DefaultValue("1") @QueryParam("page") Integer page,
			@Parameter(in = ParameterIn.QUERY, name = "sortBy", description = "Field name by which to sort", schema = @Schema(type = SchemaType.STRING))
			@DefaultValue("diseaseAlleleDefault") @QueryParam("sortBy") String sortBy,
			@Parameter(in = ParameterIn.QUERY, name = "filter.geneName", description = "filter by gene symbol", schema = @Schema(type = SchemaType.STRING))
			@QueryParam("filter.geneName") String geneName,
			@Parameter(in = ParameterIn.QUERY, name = "filter.alleleName", description = "filter by allele symbol", schema = @Schema(type = SchemaType.STRING))
			@QueryParam("filter.alleleName") String alleleName,
			@Parameter(in = ParameterIn.QUERY, name = "filter.species", description = "filter by species", schema = @Schema(type = SchemaType.STRING))
			@QueryParam("filter.species") String species,
			@Parameter(in = ParameterIn.QUERY, name = "filter.disease", description = "filter by disease", schema = @Schema(type = SchemaType.STRING))
			@QueryParam("filter.disease") String disease,
			@Parameter(in = ParameterIn.QUERY, name = "filter.source", description = "filter by source", schema = @Schema(type = SchemaType.STRING))
			@QueryParam("filter.source") String source,
			@Parameter(in = ParameterIn.QUERY, name = "filter.reference", description = "filter by reference", schema = @Schema(type = SchemaType.STRING))
			@QueryParam("filter.reference") String reference,
			@Parameter(in = ParameterIn.QUERY, name = "filter.evidenceCode", description = "filter by evidence Code", schema = @Schema(type = SchemaType.STRING))
			@QueryParam("filter.evidenceCode") String evidenceCode,
			@Parameter(in = ParameterIn.QUERY, name = "filter.associationType", description = "filter by association type", schema = @Schema(type = SchemaType.STRING))
			@QueryParam("filter.associationType") String associationType,
			@Parameter(in = ParameterIn.QUERY, name = "asc", description = "order to sort by", schema = @Schema(type = SchemaType.STRING))
//,allowedValues = "true,false")
			@DefaultValue("true")
			@QueryParam("asc") String asc);

	@GET
	@Path("/{id}/alleles/download")
	@Produces(MediaType.TEXT_PLAIN)
	@Operation(summary = "downlaod all DiseaseAnnotation records for a given allele id")
	Response getDiseaseAnnotationsByAlleleDownload(
			@Parameter(in = ParameterIn.PATH, name = "id", description = "Search for a allele by ID", required = true, schema = @Schema(type = SchemaType.STRING))
			@PathParam("id") String id,
			@Parameter(in = ParameterIn.QUERY, name = "sortBy", description = "Field name by which to sort", schema = @Schema(type = SchemaType.STRING))
			@DefaultValue("diseaseAlleleDefault") @QueryParam("sortBy") String sortBy,
			@Parameter(in = ParameterIn.QUERY, name = "filter.geneName", description = "filter by gene symbol")
			@QueryParam("filter.geneName") String geneName,
			@Parameter(in = ParameterIn.QUERY, name = "filter.alleleName", description = "filter by allele symbol")
			@QueryParam("filter.alleleName") String alleleName,
			@Parameter(in = ParameterIn.QUERY, name = "filter.species", description = "filter by species")
			@QueryParam("filter.species") String species,
			@Parameter(in = ParameterIn.QUERY, name = "filter.disease", description = "filter by disease")
			@QueryParam("filter.disease") String disease,
			@Parameter(in = ParameterIn.QUERY, name = "filter.source", description = "filter by source")
			@QueryParam("filter.source") String source,
			@Parameter(in = ParameterIn.QUERY, name = "filter.reference", description = "filter by reference")
			@QueryParam("filter.reference") String reference,
			@Parameter(in = ParameterIn.QUERY, name = "filter.evidenceCode", description = "filter by evidence code")
			@QueryParam("filter.evidenceCode") String evidenceCode,
			@Parameter(in = ParameterIn.QUERY, name = "filter.associationType", description = "filter by association type")
			@QueryParam("filter.associationType") String associationType,
			@Parameter(in = ParameterIn.QUERY, name = "asc", description = "order to sort by", schema = @Schema(type = SchemaType.STRING))
//,allowedValues = "true,false")
			@DefaultValue("true")
			@QueryParam("asc") String asc);


	@GET
	@Path("/{id}/genes")
	@JsonView(value = {View.DiseaseAnnotationSummary.class})
	@Operation(summary = "Retrieve all DiseaseAnnotation records for a given disease id")
	JsonResultResponse<DiseaseAnnotation> getDiseaseAnnotationsByGene(
			@Parameter(in = ParameterIn.PATH, name = "id", description = "Search for a disease by ID", required = true, schema = @Schema(type = SchemaType.STRING))
			@PathParam("id") String id,
			@Parameter(in = ParameterIn.QUERY, name = "limit", description = "Number of rows returned", schema = @Schema(type = SchemaType.INTEGER))
			@DefaultValue("20") @QueryParam("limit") Integer limit,
			@Parameter(in = ParameterIn.QUERY, name = "page", description = "Page number", schema = @Schema(type = SchemaType.INTEGER))
			@DefaultValue("1") @QueryParam("page") Integer page,
			@Parameter(in = ParameterIn.QUERY, name = "sortBy", description = "order to sort by", schema = @Schema(type = SchemaType.STRING))
			@QueryParam("sortBy") String sortBy,
			@Parameter(in = ParameterIn.QUERY, name = "filter.geneName", description = "filter by gene symbol")
			@QueryParam("filter.geneName") String geneSymbol,
			@Parameter(in = ParameterIn.QUERY, name = "filter.species", description = "filter by species")
			@QueryParam("filter.species") String species,
			@Parameter(in = ParameterIn.QUERY, name = "filter.disease", description = "filter by disease")
			@QueryParam("filter.disease") String disease,
			@Parameter(in = ParameterIn.QUERY, name = "filter.provider", description = "filter by provider")
			@QueryParam("filter.provider") String provider,
			@Parameter(in = ParameterIn.QUERY, name = "filter.reference", description = "filter by reference")
			@QueryParam("filter.reference") String reference,
			@Parameter(in = ParameterIn.QUERY, name = "filter.evidenceCode", description = "filter by evidence code")
			@QueryParam("filter.evidenceCode") String evidenceCode,
			@Parameter(in = ParameterIn.QUERY, name = "filter.basedOnGeneSymbol", description = "filter by based-on-gene", schema = @Schema(type = SchemaType.STRING))
			@QueryParam("filter.basedOnGeneSymbol") String basedOnGeneSymbol,
			@Parameter(in = ParameterIn.QUERY, name = "filter.associationType", description = "filter by association type")
			@QueryParam("filter.associationType") String associationType,
			@Parameter(in = ParameterIn.QUERY, name = "asc", description = "order to sort by", schema = @Schema(type = SchemaType.STRING))
			@QueryParam("asc") String asc);


	@GET
	@Path("/{id}/genes/download")
	@Produces(MediaType.TEXT_PLAIN)
	@Operation(summary = "Retrieve all DiseaseAnnotation records for a given disease id")
	Response getDiseaseAnnotationsByGeneDownload(
			@Parameter(in = ParameterIn.PATH, name = "id", description = "Search for a disease by ID", required = true, schema = @Schema(type = SchemaType.STRING))
			@PathParam("id") String id,
			@Parameter(in = ParameterIn.QUERY, name = "sortBy", description = "order to sort by", schema = @Schema(type = SchemaType.STRING))
			@QueryParam("sortBy") String sortBy,
			@Parameter(in = ParameterIn.QUERY, name = "filter.geneName", description = "filter by gene symbol", schema = @Schema(type = SchemaType.STRING))
			@QueryParam("filter.geneName") String geneName,
			@Parameter(in = ParameterIn.QUERY, name = "filter.species", description = "filter by species", schema = @Schema(type = SchemaType.STRING))
			@QueryParam("filter.species") String species,
			@Parameter(in = ParameterIn.QUERY, name = "filter.disease", description = "filter by disease", schema = @Schema(type = SchemaType.STRING))
			@QueryParam("filter.disease") String disease,
			@Parameter(in = ParameterIn.QUERY, name = "filter.provider", description = "filter by provider", schema = @Schema(type = SchemaType.STRING))
			@QueryParam("filter.provider") String provider,
			@Parameter(in = ParameterIn.QUERY, name = "filter.reference", description = "filter by reference", schema = @Schema(type = SchemaType.STRING))
			@QueryParam("filter.reference") String reference,
			@Parameter(in = ParameterIn.QUERY, name = "filter.evidenceCode", description = "filter by evidence code", schema = @Schema(type = SchemaType.STRING))
			@QueryParam("filter.evidenceCode") String evidenceCode,
			@Parameter(in = ParameterIn.QUERY, name = "filter.basedOnGeneSymbol", description = "filter by based-on-gene", schema = @Schema(type = SchemaType.STRING))
			@QueryParam("filter.basedOnGeneSymbol") String basedOnGeneSymbol,
			@Parameter(in = ParameterIn.QUERY, name = "filter.associationType", description = "filter by association type", schema = @Schema(type = SchemaType.STRING))
			@QueryParam("filter.associationType") String associationType,
			//@ApiParam(value = "boolean for switching between table download and Download page")
			@QueryParam("fullDownload") boolean fullDownload,
			//@ApiParam(value = "download file type")
			@QueryParam("fileType") String downloadFileType,
			//@ApiParam(value = "ascending order: true or false", allowableValues = "true,false", defaultValue = "true")
			@Parameter(in = ParameterIn.QUERY, name = "asc", description = "order to sort by", schema = @Schema(type = SchemaType.STRING))
//,allowedValues = "true,false")
			@DefaultValue("true")
			@QueryParam("asc") String asc);


	@GET
	@Path("/{id}/models")
	@JsonView(value = {View.DiseaseAnnotationSummary.class})
	@Operation(summary = "Retrieve all DiseaseAnnotation records for a given disease id")
	JsonResultResponse<DiseaseAnnotation> getDiseaseAnnotationsForModel(
			@Parameter(in = ParameterIn.PATH, name = "id", description = "Search for a disease by ID", required = true, schema = @Schema(type = SchemaType.STRING))
			@PathParam("id") String id,
			@Parameter(in = ParameterIn.QUERY, name = "limit", description = "Number of rows returned", schema = @Schema(type = SchemaType.INTEGER))
			@DefaultValue("20") @QueryParam("limit") Integer limit,
			@Parameter(in = ParameterIn.QUERY, name = "page", description = "Page number", schema = @Schema(type = SchemaType.INTEGER))
			@DefaultValue("1") @QueryParam("page") Integer page,
			@Parameter(in = ParameterIn.QUERY, name = "sortBy", description = "order to sort by", schema = @Schema(type = SchemaType.STRING))
			@QueryParam("sortBy") String sortBy,
			@Parameter(in = ParameterIn.QUERY, name = "filter.modelName", description = "filter by model symbol", schema = @Schema(type = SchemaType.STRING))
			@QueryParam("filter.modelName") String modelName,
			@Parameter(in = ParameterIn.QUERY, name = "filter.geneName", description = "filter by gene symbol", schema = @Schema(type = SchemaType.STRING))
			@QueryParam("filter.geneName") String geneSymbol,
			@Parameter(in = ParameterIn.QUERY, name = "filter.species", description = "filter by species", schema = @Schema(type = SchemaType.STRING))
			@QueryParam("filter.species") String species,
			@Parameter(in = ParameterIn.QUERY, name = "filter.disease", description = "filter by disease", schema = @Schema(type = SchemaType.STRING))
			@QueryParam("filter.disease") String disease,
			@Parameter(in = ParameterIn.QUERY, name = "filter.source", description = "filter by source", schema = @Schema(type = SchemaType.STRING))
			@QueryParam("filter.source") String source,
			@Parameter(in = ParameterIn.QUERY, name = "filter.reference", description = "filter by reference", schema = @Schema(type = SchemaType.STRING))
			@QueryParam("filter.reference") String reference,
			@Parameter(in = ParameterIn.QUERY, name = "filter.evidenceCode", description = "filter by evidence code", schema = @Schema(type = SchemaType.STRING))
			@QueryParam("filter.evidenceCode") String evidenceCode,
			@Parameter(in = ParameterIn.QUERY, name = "filter.associationType", description = "filter by association type", schema = @Schema(type = SchemaType.STRING))
			@QueryParam("filter.associationType") String associationType,
			@Parameter(in = ParameterIn.QUERY, name = "asc", description = "order to sort by", schema = @Schema(type = SchemaType.STRING))
//,allowedValues = "true,false")
			@DefaultValue("true")
			@QueryParam("asc") String asc);

	@GET
	@Path("/{id}/models/download")
	@Produces(MediaType.TEXT_PLAIN)
	@Operation(summary = "Retrieve all DiseaseAnnotation records for a given disease id")
	Response getDiseaseAnnotationsForModelDownload(
			@Parameter(in = ParameterIn.PATH, name = "id", description = "Search for a disease by ID", required = true, schema = @Schema(type = SchemaType.STRING))
			@PathParam("id") String id,
			@Parameter(in = ParameterIn.QUERY, name = "sortBy", description = "order to sort by", schema = @Schema(type = SchemaType.STRING))
// allowedValues = "Default,Gene,Disease,Species")
			@DefaultValue("Gene") @QueryParam("sortBy") String sortBy,
			@Parameter(in = ParameterIn.QUERY, name = "filter.modelName", description = "filter by model name", schema = @Schema(type = SchemaType.STRING))
			@QueryParam("filter.modelName") String modelName,
			@Parameter(in = ParameterIn.QUERY, name = "filter.geneName", description = "filter by gene symbol", schema = @Schema(type = SchemaType.STRING))
			@QueryParam("filter.geneName") String geneSymbol,
			@Parameter(in = ParameterIn.QUERY, name = "filter.species", description = "filter by species", schema = @Schema(type = SchemaType.STRING))
			@QueryParam("filter.species") String species,
			@Parameter(in = ParameterIn.QUERY, name = "filter.disease", description = "filter by disease", schema = @Schema(type = SchemaType.STRING))
			@QueryParam("filter.disease") String disease,
			@Parameter(in = ParameterIn.QUERY, name = "filter.source", description = "filter by source", schema = @Schema(type = SchemaType.STRING))
			@QueryParam("filter.source") String source,
			@Parameter(in = ParameterIn.QUERY, name = "filter.reference", description = "filter by reference", schema = @Schema(type = SchemaType.STRING))
			@QueryParam("filter.reference") String reference,
			@Parameter(in = ParameterIn.QUERY, name = "filter.evidenceCode", description = "filter by evidence code", schema = @Schema(type = SchemaType.STRING))
			@QueryParam("filter.evidenceCode") String evidenceCode,
			@Parameter(in = ParameterIn.QUERY, name = "filter.associationType", description = "filter by association type", schema = @Schema(type = SchemaType.STRING))
			@QueryParam("filter.associationType") String associationType,
			@Parameter(in = ParameterIn.QUERY, name = "asc", description = "order to sort by", schema = @Schema(type = SchemaType.STRING))
//,allowedValues = "true,false")
			@DefaultValue("true")
			@QueryParam("asc") String asc);


	@GET
	@Path("/{id}/associations/download")
	@Produces(MediaType.TEXT_PLAIN)
	@Operation(summary = "Download all DiseaseAnnotation records for a given disease id and sorting / filtering parameters", hidden = true)
	Response getDiseaseAnnotationsDownloadFile(
			@Parameter(in = ParameterIn.PATH, name = "id", description = "Disease by DOID: e.g. DOID:9952", required = true, schema = @Schema(type = SchemaType.STRING))
			@PathParam("id") String id,
			@Parameter(in = ParameterIn.QUERY, name = "sortBy", description = "order to sort by", schema = @Schema(type = SchemaType.STRING))
//, allowedValues = "Default,Gene,Disease,Species")
			@DefaultValue("Gene") @QueryParam("sortBy") String sortBy,
			@Parameter(in = ParameterIn.QUERY, name = "filter.geneName", description = "filter by gene symbol", schema = @Schema(type = SchemaType.STRING))
			@QueryParam("filter.geneName") String geneName,
			@Parameter(in = ParameterIn.QUERY, name = "filter.species", description = "filter by species", schema = @Schema(type = SchemaType.STRING))
			@QueryParam("filter.species") String species,
			@Parameter(in = ParameterIn.QUERY, name = "geneticEntity", description = "filter by gene genetic Entity", schema = @Schema(type = SchemaType.STRING))
			@QueryParam("filter.geneticEntity") String geneticEntity,
			@Parameter(in = ParameterIn.QUERY, name = "filter.geneticEntityType", description = "filter by genetic Entity type", schema = @Schema(type = SchemaType.STRING))
//, allowedValues = "gene,allele")
			@QueryParam("filter.geneticEntityType") String geneticEntityType,
			@Parameter(in = ParameterIn.QUERY, name = "filter.disease", description = "filter by disease", schema = @Schema(type = SchemaType.STRING))
			@QueryParam("filter.disease") String disease,
			@Parameter(in = ParameterIn.QUERY, name = "filter.source", description = "filter by source", schema = @Schema(type = SchemaType.STRING))
			@QueryParam("filter.source") String source,
			@Parameter(in = ParameterIn.QUERY, name = "filter.reference", description = "filter by reference", schema = @Schema(type = SchemaType.STRING))
			@QueryParam("filter.reference") String reference,
			@Parameter(in = ParameterIn.QUERY, name = "filter.evidenceCode", description = "filter by evidence code", schema = @Schema(type = SchemaType.STRING))
			@QueryParam("filter.evidenceCode") String evidenceCode,
			@Parameter(in = ParameterIn.QUERY, name = "filter.basedOnGeneSymbol", description = "filter by based-on-gene", schema = @Schema(type = SchemaType.STRING))
			@QueryParam("filter.basedOnGeneSymbol") String basedOnGeneSymbol,
			@Parameter(in = ParameterIn.QUERY, name = "filter.associationType", description = "filter by association type", schema = @Schema(type = SchemaType.STRING))
			@QueryParam("filter.associationType") String associationType,
			@Parameter(in = ParameterIn.QUERY, name = "asc", description = "order to sort by", schema = @Schema(type = SchemaType.STRING))
//,allowedValues = "true,false")
			@DefaultValue("true")
			@QueryParam("asc") String asc);

	@GET
	@Path("/{id}/associations/download/all")
	@Produces(MediaType.TEXT_PLAIN)
	@Operation(description = "Retrieve all DiseaseAnnotation records for a given disease id disregarding sorting / filtering parameters", hidden = true)
	String getDiseaseAnnotationsDownload(@PathParam("id") String id);

	@GET
	@Path("")
	@JsonView(value = {View.DiseaseAnnotation.class})
	@Operation(summary = "Retrieve all disease annotations of a given set of genes")
	JsonResultResponse<JSONObject> getDiseaseAnnotationsRibbonDetails(
			@Parameter(in = ParameterIn.QUERY, name = "geneID", description = "Gene by ID", required = true)
			@QueryParam("geneID") List<String> geneIDs,
			@Parameter(in = ParameterIn.QUERY, name = "termID", description = "Term ID by which rollup should happen")
			@QueryParam("termID") String termID,
			@Parameter(in = ParameterIn.QUERY, name = "filterOptions", description = "All filter key-value pairs", schema = @Schema(type = SchemaType.STRING))
			@QueryParam("filterOptions") String filterOptions,
			@Parameter(in = ParameterIn.QUERY, name = "filter.subject.taxon", description = "Species by taxon ID", schema = @Schema(type = SchemaType.STRING))
			@QueryParam("filter.subject.taxon") String filterSpecies,
			@Parameter(in = ParameterIn.QUERY, name = "filter.gene", description = "Gene symbol", schema = @Schema(type = SchemaType.STRING))
			@QueryParam("filter.gene") String filterGene,
			@Parameter(in = ParameterIn.QUERY, name = "filter.reference", description = "Reference", schema = @Schema(type = SchemaType.STRING))
			@QueryParam("filter.reference") String filterReference,
			@Parameter(in = ParameterIn.QUERY, name = "filter.disease", description = "Ontology term name", schema = @Schema(type = SchemaType.STRING))
			@QueryParam("filter.object") String diseaseTerm,
			@Parameter(in = ParameterIn.QUERY, name = "filter.source", description = "Source", schema = @Schema(type = SchemaType.STRING))
			@QueryParam("filter.provider") String filterSource,
			@Parameter(in = ParameterIn.QUERY, name = "filter.geneticEntity", description = "geneticEntity", schema = @Schema(type = SchemaType.STRING))
			@QueryParam("filter.geneticEntity") String geneticEntity,
			@Parameter(in = ParameterIn.QUERY, name = "filter.geneticEntityType", description = "geneticEntityType", schema = @Schema(type = SchemaType.STRING))
			@QueryParam("filter.geneticEntityType") String geneticEntityType,
			@Parameter(in = ParameterIn.QUERY, name = "filter.associationType", description = "associationType", schema = @Schema(type = SchemaType.STRING))
			@QueryParam("filter.diseaseRelation.name") String associationType,
			@Parameter(in = ParameterIn.QUERY, name = "filter.evidenceCode", description = "evidenceCode", schema = @Schema(type = SchemaType.STRING))
			@QueryParam("filter.evidenceCode") String evidenceCode,
			@Parameter(in = ParameterIn.QUERY, name = "filter.basedOnGeneSymbol", description = "basedOnGeneSymbol", schema = @Schema(type = SchemaType.STRING))
			@QueryParam("filter.basedOnGeneSymbol") String basedOnGeneSymbol,
			@Parameter(in = ParameterIn.QUERY, name = "includeNegation", description = "include negated annotations", schema = @Schema(type = SchemaType.STRING))
			@DefaultValue("false") @QueryParam("includeNegation") String includeNegation,
			@Parameter(in = ParameterIn.QUERY, name = "limit", description = "Number of rows returned", schema = @Schema(type = SchemaType.INTEGER))
			@DefaultValue("20") @QueryParam("limit") Integer limit,
			@Parameter(in = ParameterIn.QUERY, name = "page", description = "Page number")
			@DefaultValue("1") @QueryParam("page") Integer page,
			@Parameter(in = ParameterIn.QUERY, name = "sortBy", description = "Sort by field name")
			@QueryParam("sortBy") String sortBy,
			@Parameter(in = ParameterIn.QUERY, name = "asc", description = "ascending or descending", schema = @Schema(type = SchemaType.STRING))
//allowedValues = "true,false",
			@DefaultValue("true") @QueryParam("asc") String asc
	) throws JsonProcessingException;

	@GET
	@Path("/download")
	@JsonView(value = {View.DiseaseAnnotation.class})
	@Operation(summary = "Download all disease annotations of a given set of genes")
	Response getDiseaseAnnotationsRibbonDetailsDownload(
			@Parameter(in = ParameterIn.QUERY, name = "geneID", description = "Gene by ID", required = true)
			@QueryParam("geneID") List<String> geneIDs,
			@Parameter(in = ParameterIn.QUERY, name = "termID", description = "Term ID by which rollup should happen")
			@QueryParam("termID") String termID,
			@Parameter(in = ParameterIn.QUERY, name = "filter.species", description = "Species by taxon ID", schema = @Schema(type = SchemaType.STRING))
			@QueryParam("filter.species") String filterSpecies,
			@Parameter(in = ParameterIn.QUERY, name = "filter.gene", description = "Gene symbol", schema = @Schema(type = SchemaType.STRING))
			@QueryParam("filter.gene") String filterGene,
			@Parameter(in = ParameterIn.QUERY, name = "filter.reference", description = "Reference", schema = @Schema(type = SchemaType.STRING))
			@QueryParam("filter.reference") String filterReference,
			@Parameter(in = ParameterIn.QUERY, name = "filter.disease", description = "Ontology term name", schema = @Schema(type = SchemaType.STRING))
			@QueryParam("filter.disease") String diseaseTerm,
			@Parameter(in = ParameterIn.QUERY, name = "filter.source", description = "Source", schema = @Schema(type = SchemaType.STRING))
			@QueryParam("filter.provider") String filterSource,
			@Parameter(in = ParameterIn.QUERY, name = "filter.geneticEntity", description = "geneticEntity", schema = @Schema(type = SchemaType.STRING))
			@QueryParam("filter.geneticEntity") String geneticEntity,
			@Parameter(in = ParameterIn.QUERY, name = "filter.geneticEntityType", description = "geneticEntityType", schema = @Schema(type = SchemaType.STRING))
			@QueryParam("filter.geneticEntityType") String geneticEntityType,
			@Parameter(in = ParameterIn.QUERY, name = "filter.associationType", description = "associationType", schema = @Schema(type = SchemaType.STRING))
			@QueryParam("filter.associationType") String associationType,
			@Parameter(in = ParameterIn.QUERY, name = "filter.evidenceCode", description = "evidenceCode", schema = @Schema(type = SchemaType.STRING))
			@QueryParam("filter.evidenceCode") String evidenceCode,
			@Parameter(in = ParameterIn.QUERY, name = "filter.basedOnGeneSymbol", description = "basedOnGeneSymbol", schema = @Schema(type = SchemaType.STRING))
			@QueryParam("filter.basedOnGeneSymbol") String basedOnGeneSymbol,
			@Parameter(in = ParameterIn.QUERY, name = "includeNegation", description = "include negated annotations", schema = @Schema(type = SchemaType.STRING))
			@DefaultValue("false") @QueryParam("includeNegation") String includeNegation,
			@Parameter(in = ParameterIn.QUERY, name = "sortBy", description = "Sort by field name")
			@QueryParam("sortBy") String sortBy,
			@Parameter(in = ParameterIn.QUERY, name = "asc", description = "ascending or descending", schema = @Schema(type = SchemaType.STRING))
//allowedValues = "true,false"
			@DefaultValue("true") @QueryParam("asc") String asc
	) throws JsonProcessingException;

	@GET
	@Path("/annotation/download")
	@Operation(summary = "Download all disease annotations of a given set of species", hidden = true)
	Response getDiseaseAnnotationsBySpeciesDownload(
			@Parameter(in = ParameterIn.PATH, name = "species", description = "Species by ID", required = true)
			@QueryParam("species") List<String> species,
			@Parameter(in = ParameterIn.PATH, name = "diseaseID", description = "Disease ID")
			@DefaultValue("DOID:4") @QueryParam("diseaseID") String diseaseID,
			@Parameter(in = ParameterIn.PATH, name = "sortBy", description = "Sort by field name")
			@QueryParam("sortBy") String sortBy
	) throws JsonProcessingException;

}
