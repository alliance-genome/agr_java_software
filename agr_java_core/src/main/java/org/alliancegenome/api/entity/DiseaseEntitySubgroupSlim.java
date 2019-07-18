package org.alliancegenome.api.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.Setter;
import lombok.Getter;
import org.alliancegenome.neo4j.view.View;

@Setter
@Getter
public class DiseaseEntitySubgroupSlim {

    private String id;
    @JsonView(View.DiseaseAnnotation.class)
    @JsonProperty("nb_classes")
    private int numberOfClasses;
    @JsonView(View.DiseaseAnnotation.class)
    @JsonProperty("nb_annotations")
    private int numberOfAnnotations;

}
