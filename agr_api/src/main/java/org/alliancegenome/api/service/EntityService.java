package org.alliancegenome.api.service;

import java.util.Map;

import jakarta.enterprise.context.RequestScoped;

@RequestScoped
public class EntityService {

	public Map<String, Object> getById(String id) {
		// Needs to be Implemented in Neo
		return null;
	}

}
