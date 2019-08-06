package org.alliancegenome.api.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.Getter;
import lombok.Setter;
import org.alliancegenome.neo4j.view.View;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
public class RibbonSummary implements Serializable {

    public static final String ALL_ANNOTATIONS = "ALL:allAnnotations";
    public static final String OTHER = "DOID:Other";
    @JsonView({View.DiseaseAnnotation.class, View.Expression.class})
    @JsonProperty("categories")
    private List<RibbonSection> diseaseRibbonSections = new ArrayList<>();

    @JsonView({View.DiseaseAnnotation.class, View.Expression.class})
    @JsonProperty("subjects")
    private List<RibbonEntity> diseaseRibbonEntities = new ArrayList<>();

    public void addRibbonSection(RibbonSection section) {
        diseaseRibbonSections.add(section);
    }

    public void addRibbonEntity(RibbonEntity entity) {
        diseaseRibbonEntities.add(entity);
    }

    public RibbonSummary() {
    }

    protected RibbonSummary clone() throws CloneNotSupportedException {
        RibbonSummary clone;
        clone = (RibbonSummary) super.clone();
        return clone;
    }
}
