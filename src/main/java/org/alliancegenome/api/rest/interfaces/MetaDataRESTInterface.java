package org.alliancegenome.api.rest.interfaces;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.alliancegenome.es.index.data.document.MetaDataDocument;
import org.alliancegenome.es.index.data.document.SubmissionResponce;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

@Path("/data")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface MetaDataRESTInterface {

    @GET
    @Path("/meta")
    public MetaDataDocument getMetaData(String release);
    
    @GET
    @Path("/snapshot")
    public MetaDataDocument getSnapShot(String snapShotName);
    
    @POST
    @Path("/snapshot")
    public MetaDataDocument takeSnapShot();

    @POST
    @Path("/submit")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public SubmissionResponce submitData(@HeaderParam("api_access_token") String api_access_token, MultipartFormDataInput input);

    @POST
    @Path("/validate")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public SubmissionResponce validateData(@HeaderParam("api_access_token") String api_access_token, MultipartFormDataInput input);

}
