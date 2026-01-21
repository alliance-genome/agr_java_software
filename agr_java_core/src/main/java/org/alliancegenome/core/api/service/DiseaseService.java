package org.alliancegenome.core.api.service;

import static java.util.stream.Collectors.groupingBy;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.alliancegenome.neo4j.entity.DiseaseSummary;
import org.alliancegenome.neo4j.entity.PrimaryAnnotatedEntity;
import org.alliancegenome.neo4j.entity.node.DOTerm;
import org.alliancegenome.neo4j.entity.node.ExperimentalCondition;
import org.alliancegenome.neo4j.repository.DiseaseRepository;
import org.apache.commons.collections.MapUtils;

import jakarta.enterprise.context.RequestScoped;

@RequestScoped
public class DiseaseService {

	private static DiseaseRepository diseaseRepository = new DiseaseRepository();

	public DOTerm getById(String id) {
		return diseaseRepository.getDiseaseTerm(id);
	}

	public Map<String, Map<String, List<PrimaryAnnotatedEntity>>> getGroupedByMap(List<PrimaryAnnotatedEntity> entityList) {
		return entityList.stream()
			.collect(groupingBy(PrimaryAnnotatedEntity::getId,
				groupingBy(t -> {
						if (MapUtils.isNotEmpty(t.getConditions())) {
							Map.Entry<String, List<ExperimentalCondition>> conditionType = t.getConditions().entrySet().iterator().next();
							StringBuilder key = new StringBuilder(conditionType.getKey() + ":");
							conditionType.getValue().stream().sorted(Comparator.comparing(ExperimentalCondition::getConditionStatement)).forEach(experimentalCondition -> {
								key.append(experimentalCondition.getConditionStatement()).append(":");
							});
							return key.toString();
						} else {
							return "No-ExperimentalConditions";
						}
					}
				)));
	}

	public DiseaseSummary getDiseaseSummary(String id, DiseaseSummary.Type type) {
		return diseaseRepository.getDiseaseSummary(id, type);
	}



}
