package org.alliancegenome.api.entity;

import org.alliancegenome.neo4j.view.View;

import com.fasterxml.jackson.annotation.*;

import lombok.*;

@Setter
@Getter
public class EntitySubgroupSlim {

    private String id;
    @JsonView({View.DiseaseAnnotation.class, View.Expression.class})
    @JsonProperty("nb_classes")
    private int numberOfClasses;
    @JsonView({View.DiseaseAnnotation.class, View.Expression.class})
    @JsonProperty("nb_annotations")
    private int numberOfAnnotations;

    private Boolean available;

    public void setAvailable(Boolean available) {
        // only set this variable if it is false.
        // if it is true it is covered by the default behavior in the ribbon code
        if (available != null && !available)
            this.available = available;
    }
}
