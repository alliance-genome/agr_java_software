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
public class DiseaseRibbonEntity {

    @JsonView(View.DiseaseAnnotation.class)
    private String id;
    @JsonView(View.DiseaseAnnotation.class)
    private String label;
    @JsonView(View.DiseaseAnnotation.class)
    private String taxonID;
    @JsonView(View.DiseaseAnnotation.class)
    private String annotationLabel;

    @JsonView(View.DiseaseAnnotation.class)
    @JsonProperty("groups")
    private List<DiseaseEntitySlim> slims = new ArrayList<>();

    public void addDiseaseSlim(DiseaseEntitySlim slim) {
        slims.add(slim);
    }
}
