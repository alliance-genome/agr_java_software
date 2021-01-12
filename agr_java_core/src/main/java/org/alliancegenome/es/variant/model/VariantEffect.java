package org.alliancegenome.es.variant.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class VariantEffect {
    private String consequence;
    private String featureType;
}
