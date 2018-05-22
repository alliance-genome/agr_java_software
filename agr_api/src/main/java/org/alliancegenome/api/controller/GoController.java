package org.alliancegenome.api.controller;

import java.util.Map;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.NotFoundException;

import org.alliancegenome.api.rest.interfaces.GoRESTInterface;
import org.alliancegenome.api.service.GoService;

@RequestScoped
public class GoController extends BaseController implements GoRESTInterface {

    @Inject
    private GoService goService;

    @Override
    public Map<String, Object> getGo(String id) {
        Map<String, Object> ret = goService.getById(id);
        if(ret == null) {
            throw new NotFoundException();
        } else {
            return ret;
        }
    }

}
