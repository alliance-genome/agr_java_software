package org.alliancegenome.core.api.service;

import org.alliancegenome.neo4j.entity.DiseaseSummary;
import org.alliancegenome.neo4j.entity.node.DOTerm;
import org.alliancegenome.neo4j.repository.DiseaseRepository;

import jakarta.enterprise.context.RequestScoped;

@RequestScoped
public class DiseaseService {

	private static DiseaseRepository diseaseRepository = new DiseaseRepository();

	public DOTerm getById(String id) {
		return diseaseRepository.getDiseaseTerm(id);
	}


	public DiseaseSummary getDiseaseSummary(String id, DiseaseSummary.Type type) {
		return diseaseRepository.getDiseaseSummary(id, type);
	}


}
