package org.alliancegenome.es.variant.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.*;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class Sample {
    private String sampleName;
    private int depth;
    private String type;
}
