package org.alliancegenome.api.config;

import io.swagger.jaxrs.listing.ApiListingResource;
import io.swagger.jaxrs.listing.SwaggerSerializers;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Produces;
import javax.ws.rs.Path;

@RequestScoped
@Path("/swagger.{type:json|yaml}")
public class SwaggerApiListing extends ApiListingResource {
    @Produces
    @RequestScoped
    private SwaggerSerializers serializers = new SwaggerSerializers();
}
