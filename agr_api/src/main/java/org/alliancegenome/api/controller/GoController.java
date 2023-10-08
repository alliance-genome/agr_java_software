package org.alliancegenome.api.controller;

import org.alliancegenome.api.rest.interfaces.GoRESTInterface;
import org.alliancegenome.api.service.GoService;
import org.alliancegenome.neo4j.entity.node.GOTerm;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

@RequestScoped
public class GoController implements GoRESTInterface {

	@Inject GoService goService;

	@Override
	public GOTerm getGo(String id) {
		return goService.getById(id);
	}

}
