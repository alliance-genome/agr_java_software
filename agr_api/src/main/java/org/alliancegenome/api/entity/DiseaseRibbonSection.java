package org.alliancegenome.api.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.Getter;
import lombok.Setter;
import org.alliancegenome.neo4j.view.View;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class DiseaseRibbonSection {

    @JsonView({View.DiseaseAnnotation.class})
    private String id;
    @JsonView({View.DiseaseAnnotation.class})
    private String label;
    @JsonProperty("class_label")
    private String classLabel;
    @JsonProperty("annotation_label")
    private String annotationLabel;
    @JsonView({View.DiseaseAnnotation.class})
    private String taxonId;

    @JsonView({View.DiseaseAnnotation.class})
    @JsonProperty("groups")
    private List<DiseaseSectionSlim> slims = new ArrayList<>();

    public void addDiseaseSlim(DiseaseSectionSlim slim) {
        slims.add(slim);
    }
}
