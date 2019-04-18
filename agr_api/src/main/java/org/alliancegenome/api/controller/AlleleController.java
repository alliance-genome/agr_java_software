package org.alliancegenome.api.controller;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import org.alliancegenome.api.rest.interfaces.AlleleRESTInterface;
import org.alliancegenome.api.service.AlleleService;
import org.alliancegenome.core.service.JsonResultResponse;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.neo4j.entity.node.Allele;

@RequestScoped
public class AlleleController implements AlleleRESTInterface {

    @Inject
    private AlleleService alleleService;

    @Inject
    private HttpServletRequest request;

    @Override
    public Allele getAllele(String id) {
        return alleleService.getById(id);
    }

    @Override
    public JsonResultResponse<Allele> getAllelesPerSpecies(String species, int limit, int page, String sortBy, String asc) {
        long startTime = System.currentTimeMillis();
        Pagination pagination = new Pagination(page, limit, sortBy, asc);
        JsonResultResponse<Allele> response = alleleService.getAllelesBySpecies(species, pagination);
        response.setHttpServletRequest(request);
        Long duration = (System.currentTimeMillis() - startTime) / 1000;
        response.setRequestDuration(duration.toString());
        return response;
    }

}
