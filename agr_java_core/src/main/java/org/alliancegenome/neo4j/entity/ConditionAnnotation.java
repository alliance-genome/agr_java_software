package org.alliancegenome.neo4j.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.Getter;
import lombok.Setter;
import org.alliancegenome.api.entity.PresentationEntity;
import org.alliancegenome.neo4j.entity.node.*;
import org.alliancegenome.neo4j.view.View;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

@Getter
@Setter
@Schema(name = "ConditionAnnotation", description = "POJO that represents a Condition Annotation")
public abstract class ConditionAnnotation  {

    private Map<String, ExperimentalCondition> conditions;

    private Map<String, ExperimentalCondition> conditionModifiers;

    @JsonView({View.DiseaseAnnotation.class,View.PrimaryAnnotation.class})
    public Map<String, ExperimentalCondition> getConditionModifiers() {
        return conditionModifiers;
    }

    public void addModifier(ConditionType conditionType, List<ExperimentalCondition> conditionModifier) {
        if (conditionModifier == null || conditionType == null)
            return;
        if (!conditionType.isModifier())
            throw new RuntimeException("No Modifier condition provided:" + conditionType);
        if (this.conditionModifiers == null)
            this.conditionModifiers = new HashMap<>();
        conditionModifier.forEach(condition -> conditionModifiers.put(conditionType.name(), condition));
    }

    public void setConditions(Map<String, ExperimentalCondition> conditions) {
        this.conditions = conditions;
    }

    public void setConditionModifiers(Map<String, ExperimentalCondition> conditionModifiers) {
        this.conditionModifiers = conditionModifiers;
    }

    @JsonView({View.DiseaseAnnotation.class,View.PrimaryAnnotation.class})
    public Map<String, ExperimentalCondition> getConditions() {
        return conditions;
    }

    public void addConditions(ConditionType conditionType, List<ExperimentalCondition> conditions) {
        if (conditions == null || conditionType == null)
            return;
        if (!conditionType.isCondition())
            throw new RuntimeException("No condition type provided:" + conditionType);
        if (this.conditions == null)
            this.conditions = new HashMap<>();
        conditions.forEach(condition -> this.conditions.put(conditionType.name(), condition));
    }

    public enum ConditionType {
        HAS_CONDITION, INDUCES, AMELIORATES, EXACERBATES;

        public static boolean valueOfIgnoreCase(String conditionType) {
            return Arrays.stream(values()).anyMatch(conditionType1 -> conditionType1.name().equalsIgnoreCase(conditionType));
        }

        public boolean isModifier() {
            return this.equals(AMELIORATES) || this.equals(EXACERBATES);
        }

        public boolean isCondition() {
            return this.equals(HAS_CONDITION) || this.equals(INDUCES);
        }
    }


}
