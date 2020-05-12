package org.alliancegenome.api.rest.interfaces;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.alliancegenome.api.service.ensembl.model.EnsemblVariant;
import org.alliancegenome.api.service.ensembl.model.EnsemblVariantConsequence;
import org.alliancegenome.cache.repository.helper.JsonResultResponse;
import org.alliancegenome.neo4j.view.View;

import com.fasterxml.jackson.annotation.JsonView;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@Path("ensemblvariants")
@Api(value = "Ensembl Variants Search")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface EnsemblVariantRESTInterface {
    
    @GET
    @Path("/get/{id}")
    @JsonView(value = {View.Default.class})
    @ApiOperation(value = "Retrieve all expression records of a given set of geneMap")
    JsonResultResponse<EnsemblVariant> getEnsemblVariants(
        @ApiParam(name = "id", value = "Search for Ensembl Variants for a given Gene by ID")
        @PathParam("id") String id
    );
    
    @GET
    @Path("/get/{id}/vep")
    @JsonView(value = {View.Default.class})
    @ApiOperation(value = "Retrieve all expression records of a given set of geneMap")
    JsonResultResponse<EnsemblVariantConsequence> getEnsemblVariantsVEP(
        @ApiParam(name = "id", value = "Search for Ensembl Variants VEP Data for a given Gene by ID")
        @PathParam("id") String id
    );

}
