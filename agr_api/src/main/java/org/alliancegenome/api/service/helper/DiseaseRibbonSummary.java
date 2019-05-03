package org.alliancegenome.api.service.helper;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.Getter;
import lombok.Setter;
import org.alliancegenome.api.entity.DiseaseRibbonEntity;
import org.alliancegenome.api.entity.DiseaseRibbonSection;
import org.alliancegenome.neo4j.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
}
