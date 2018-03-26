package org.alliancegenome.api.controller;

import org.alliancegenome.api.rest.interfaces.AutoCompleteRESTInterface;
import org.alliancegenome.api.service.AutoCompleteService;
import org.alliancegenome.es.model.search.AutoCompleteResult;
import org.jboss.logging.Logger;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

@RequestScoped
public class AutoCompleteController extends BaseController implements AutoCompleteRESTInterface {

    @Inject
    private AutoCompleteService autoCompleteService;

    private Logger log = Logger.getLogger(getClass());

    @Override
    public AutoCompleteResult searchAutoComplete(String q, String category) {
        log.info("This is the Auto Complete query: " + q);
        return autoCompleteService.buildQuery(q, category);
    }

}