package org.alliancegenome.api.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.Getter;
import lombok.Setter;
import org.alliancegenome.neo4j.view.View;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

    // return the last section
    @JsonIgnore
    public RibbonSection getOtherSection() {
        return diseaseRibbonSections.get(diseaseRibbonSections.size() - 1);
    }

    public void addAllAnnotationsCount(String geneID, int totalNumber) {
        Optional<RibbonEntity> entity = diseaseRibbonEntities.stream()
                .filter(ribbonEntity -> ribbonEntity.getId().equals(geneID))
                .findFirst();
        if (!entity.isPresent())
            throw new RuntimeException("No ribbon entity for gene " + geneID);
        entity.get().setNumberOfAnnotations(totalNumber);
    }

    public RibbonSummary() {
    }

    protected RibbonSummary clone() throws CloneNotSupportedException {
        RibbonSummary clone = null;
        clone = (RibbonSummary) super.clone();
        return clone;
    }
}
