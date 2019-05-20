package org.alliancegenome.api.entity;

import com.fasterxml.jackson.annotation.JsonView;
import lombok.Setter;
import lombok.Getter;
import org.alliancegenome.neo4j.view.View;

@Setter
@Getter
public class DiseaseEntitySubgroupSlim {

    private String id;
/*
    @JsonView(View.DiseaseAnnotation.class)
    private String groupName;
*/
    @JsonView(View.DiseaseAnnotation.class)
    private int numberOfClasses;
    @JsonView(View.DiseaseAnnotation.class)
    private int numberOfAnnotations;

}
