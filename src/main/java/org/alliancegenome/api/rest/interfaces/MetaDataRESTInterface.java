package org.alliancegenome.api.rest.interfaces;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.alliancegenome.shared.es.document.data_index.MetaDataDocument;
import org.alliancegenome.shared.es.document.data_index.SubmissionResponce;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/data")
@Api(value = "Meta Data")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface MetaDataRESTInterface {

    @GET
    @Path("/meta")
    @ApiOperation(value = "Get MetaData from Build", notes="Meta Data Notes")
    public MetaDataDocument getMetaData();

    @POST
    @Path("/submit")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @ApiOperation(value = "Submit new data file for build", notes = "Data file submission")
    public SubmissionResponce submitData(@HeaderParam("api_access_token") String api_access_token, MultipartFormDataInput input);

    @POST
    @Path("/validate")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @ApiOperation(value = "Validate only new data file for build", notes = "Data file validation")
    public SubmissionResponce validateData(@HeaderParam("api_access_token") String api_access_token, MultipartFormDataInput input);

}
