package org.alliancegenome.agr_submission.main.migrationmodels;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

@Path("/data_index")
public interface ESResultInterface {

    @GET
    @Path("/_search")
    public ESResults getResults(
            @HeaderParam("Authorization") String authorization,
            @QueryParam("size") Integer size
    );
    
}
