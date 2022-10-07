package org.alliancegenome.indexer;

import org.alliancegenome.curation_api.model.entities.AGMDiseaseAnnotation;
import org.alliancegenome.curation_api.model.entities.AlleleDiseaseAnnotation;
import org.alliancegenome.curation_api.model.entities.GeneDiseaseAnnotation;
import org.alliancegenome.curation_api.response.SearchResponse;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@Path("/api")
public interface DiseaseAnnotationRESTInterface {
    @POST
    @Path("/agm-disease-annotation/find")
    @Produces({MediaType.APPLICATION_JSON})
    SearchResponse<AGMDiseaseAnnotation> getAgmDiseaseAnnotation(@HeaderParam("Authorization") String auth,
                                                                 @QueryParam("limit") Integer limit);

    @POST
    @Path("/gene-disease-annotation/find")
    @Produces({MediaType.APPLICATION_JSON})
    SearchResponse<GeneDiseaseAnnotation> getGeneDiseaseAnnotation(@HeaderParam("Authorization") String auth,
                                                                   @QueryParam("limit") Integer limit);

    @POST
    @Path("/allele-disease-annotation/find")
    @Produces({MediaType.APPLICATION_JSON})
    SearchResponse<AlleleDiseaseAnnotation> getAlleleDiseaseAnnotation(@HeaderParam("Authorization") String auth,
                                                                       @QueryParam("limit") Integer limit);

}
