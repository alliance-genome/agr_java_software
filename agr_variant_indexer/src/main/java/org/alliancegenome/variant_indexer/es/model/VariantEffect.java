package org.alliancegenome.variant_indexer.es.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VariantEffect {
    private String consequence;
    private String featureType;
}
