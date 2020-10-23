package org.alliancegenome.variant_indexer.es.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class Sample {
    private String sampleName;
    private int depth;
    private String type;
}
