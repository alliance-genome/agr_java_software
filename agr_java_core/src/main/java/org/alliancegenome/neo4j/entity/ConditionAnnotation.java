package org.alliancegenome.neo4j.entity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alliancegenome.neo4j.entity.node.ExperimentalCondition;
import org.alliancegenome.neo4j.view.View;
import org.apache.commons.collections.MapUtils;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonView;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(name = "ConditionAnnotation", description = "POJO that represents a Condition Annotation")
public abstract class ConditionAnnotation {

	@JsonView({View.DiseaseAnnotation.class, View.PrimaryAnnotation.class, View.PhenotypeAPI.class})
	private Map<String, List<ExperimentalCondition>> conditions;

	@JsonView({View.DiseaseAnnotation.class, View.PrimaryAnnotation.class, View.PhenotypeAPI.class})
	private Map<String, List<ExperimentalCondition>> conditionModifiers;


	public void addCondition(ConditionType conditionType, List<ExperimentalCondition> conditions) {
		if (conditions == null || conditionType == null)
			return;
		if (!conditionType.isCondition())
			throw new RuntimeException("No condition type provided:" + conditionType);
		if (this.conditions == null)
			this.conditions = new HashMap<>();
		this.conditions.computeIfAbsent(conditionType.getDisplayName(), k -> new ArrayList<>());
		this.conditions.get(conditionType.getDisplayName()).addAll(conditions);
	}
	
	public void addModifier(ConditionType conditionType, List<ExperimentalCondition> conditionModifier) {
		if (conditionModifier == null || conditionType == null)
			return;
		if (!conditionType.isModifier())
			throw new RuntimeException("No Modifier condition provided:" + conditionType);
		if (this.conditionModifiers == null)
			this.conditionModifiers = new HashMap<>();
		this.conditionModifiers.computeIfAbsent(conditionType.getDisplayName(), k -> new ArrayList<>());
		this.conditionModifiers.get(conditionType.getDisplayName()).addAll(conditionModifier);
	}

	public boolean hasExperimentalCondition() {
		return MapUtils.isNotEmpty(conditions) || MapUtils.isNotEmpty(conditionModifiers);
	}

	public enum ConditionType {
		HAS_CONDITION("has_condition"),
		INDUCES("induced_by"),
		AMELIORATES("ameliorated_by"),
		EXACERBATES("exacerbated_by");

		String displayName;

		ConditionType(String displayName) {
			this.displayName = displayName;
		}

		public static boolean valueOfIgnoreCase(String conditionType) {
			return Arrays.stream(values()).anyMatch(conditionType1 -> conditionType1.getDisplayName().equalsIgnoreCase(conditionType));
		}

		public boolean isModifier() {
			return this.equals(AMELIORATES) || this.equals(EXACERBATES);
		}

		public boolean isCondition() {
			return this.equals(HAS_CONDITION) || this.equals(INDUCES);
		}

		public String getDisplayName() {
			return displayName;
		}
	}


}
