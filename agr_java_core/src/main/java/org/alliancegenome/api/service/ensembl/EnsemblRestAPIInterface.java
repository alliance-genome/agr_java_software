package org.alliancegenome.api.service.ensembl;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.alliancegenome.api.service.ensembl.model.EnsemblVariant;
import org.alliancegenome.api.service.ensembl.model.EnsemblVariantConsequence;
import org.alliancegenome.api.service.ensembl.model.EnsemblVariantFeature;
import org.alliancegenome.api.service.ensembl.model.VariantListForm;
import org.alliancegenome.neo4j.view.View;

import com.fasterxml.jackson.annotation.JsonView;

@Path("/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface EnsemblRestAPIInterface {

    @GET
    @Path("variation/{species}/{id}")
    public EnsemblVariantFeature getVariantFeatures(
            @PathParam("id") String id,
            @PathParam("species") String species,
            @QueryParam("genotypes") Integer genotypes
    );

    @GET
    @Path("vep/{species}/id/{id}")
    public List<EnsemblVariantConsequence> getVariantConsequence(
            @PathParam("id") String id,
            @PathParam("species") String species
    );
    
    @GET
    @Path("overlap/id/{id}")
    @JsonView({View.Default.class})
    public List<EnsemblVariant> getVariants(
            @PathParam("id") String id,
            @QueryParam("feature") String feature
    );
    
    @POST
    @Path("vep/{species}/id")
    @JsonView({View.Default.class})
    public List<EnsemblVariantConsequence> getVariantConsequences(
            @PathParam("species") String species,
            VariantListForm form
            //@FormParam("ids") List<String> ids
    );
}
