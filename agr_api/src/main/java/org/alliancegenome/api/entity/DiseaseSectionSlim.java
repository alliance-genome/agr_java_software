package org.alliancegenome.api.entity;

import com.fasterxml.jackson.annotation.JsonView;
import lombok.Getter;
import lombok.Setter;
import org.alliancegenome.neo4j.view.View;

@Getter
@Setter
public class DiseaseSectionSlim {

    @JsonView({View.DiseaseAnnotation.class})
    private String id;
    @JsonView({View.DiseaseAnnotation.class})
    private String label;
    @JsonView({View.DiseaseAnnotation.class})
    private String classLabel;

}
