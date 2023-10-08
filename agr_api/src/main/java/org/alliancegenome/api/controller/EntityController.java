package org.alliancegenome.api.controller;

import java.util.Map;

import org.alliancegenome.api.rest.interfaces.EntityRESTInterface;
import org.alliancegenome.api.service.EntityService;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

@RequestScoped
public class EntityController implements EntityRESTInterface {

	@Inject EntityService entityService;

	@Override
	public Map<String, Object> getEntity(String id) {
		return entityService.getById(id);
	}

}
