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
public class DiseaseRibbonSummary implements Serializable {

    public static final String DOID_ALL_ANNOTATIONS = "DOID:allAnnotations";
    public static final String DOID_OTHER = "DOID:Other";
    @JsonView(View.DiseaseAnnotation.class)
    @JsonProperty("categories")
    private List<RibbonSection> diseaseRibbonSections = new ArrayList<>();

    @JsonView(View.DiseaseAnnotation.class)
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
                .filter(diseaseRibbonEntity -> diseaseRibbonEntity.getId().equals(geneID))
                .findFirst();
        if (!entity.isPresent())
            throw new RuntimeException("No ribbon entity for gene " + geneID);
        EntitySubgroupSlim group = new EntitySubgroupSlim();
        group.setNumberOfAnnotations(totalNumber);
        group.setId(geneID);
        entity.get().addEntitySlim(group);
    }

    public DiseaseRibbonSummary() {
    }

    protected DiseaseRibbonSummary clone() throws CloneNotSupportedException {
        DiseaseRibbonSummary clone = null;
        clone = (DiseaseRibbonSummary) super.clone();
        return clone;
    }
}
