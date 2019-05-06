package org.alliancegenome.api.service.helper;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.Getter;
import lombok.Setter;
import org.alliancegenome.api.entity.*;
import org.alliancegenome.neo4j.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Setter
@Getter
public class DiseaseRibbonSummary {

    @JsonView(View.DiseaseAnnotation.class)
    @JsonProperty("section")
    private List<DiseaseRibbonSection> diseaseRibbonSections = new ArrayList<>();

    @JsonView(View.DiseaseAnnotation.class)
    @JsonProperty("entity")
    private List<DiseaseRibbonEntity> diseaseRibbonEntities = new ArrayList<>();

    public void addRibbonSection(DiseaseRibbonSection section) {
        diseaseRibbonSections.add(section);
    }

    public void addRibbonEntity(DiseaseRibbonEntity entity) {
        diseaseRibbonEntities.add(entity);
    }

    // return the last section
    public DiseaseRibbonSection getOtherSection() {
        return diseaseRibbonSections.get(diseaseRibbonSections.size() - 1);
    }

    public void addAllAnnotationsCount(String geneID, int totalNumber) {
        Optional<DiseaseRibbonEntity> entity = diseaseRibbonEntities.stream()
                .findFirst().filter(diseaseRibbonEntity -> diseaseRibbonEntity.getId().equals(geneID));
        if (!entity.isPresent())
            throw new RuntimeException("No ribbon entity for gene " + geneID);
        DiseaseEntitySubgroupSlim group = new DiseaseEntitySubgroupSlim();
        group.setGroupName("Disease Annotations");
        group.setNumberOfAnnotations(totalNumber);
        DiseaseEntitySlim slim = new DiseaseEntitySlim();
        slim.addDiseaseEntitySubgroupSlim(group);
        entity.get().getSlims().add(0, slim);
    }

    public DiseaseRibbonSummary() {
        DiseaseRibbonSection allAnnotations = new DiseaseRibbonSection();
        final String allAnnotation = "All annotations";
        allAnnotations.setLabel(allAnnotation);
        DiseaseSectionSlim slim = new DiseaseSectionSlim();
        slim.setLabel(allAnnotation);
        allAnnotations.addDiseaseSlim(slim);
        addRibbonSection(allAnnotations);
    }
}
