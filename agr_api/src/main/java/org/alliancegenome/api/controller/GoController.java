package org.alliancegenome.api.controller;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import org.alliancegenome.api.rest.interfaces.GoRESTInterface;
import org.alliancegenome.api.service.GoService;
import org.alliancegenome.neo4j.entity.node.GOTerm;

@RequestScoped
public class GoController implements GoRESTInterface {

	@Inject GoService goService;

	@Override
	public GOTerm getGo(String id) {
		return goService.getById(id);
	}

}
