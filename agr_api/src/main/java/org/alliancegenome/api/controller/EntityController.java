package org.alliancegenome.api.controller;

import java.util.Map;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import org.alliancegenome.api.rest.interfaces.EntityRESTInterface;
import org.alliancegenome.api.service.EntityService;

@RequestScoped
public class EntityController implements EntityRESTInterface {

	@Inject EntityService entityService;

	@Override
	public Map<String, Object> getEntity(String id) {
		return entityService.getById(id);
	}

}
