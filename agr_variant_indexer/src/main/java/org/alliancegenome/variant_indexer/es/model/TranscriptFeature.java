package org.alliancegenome.variant_indexer.es.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TranscriptFeature {
    private String feature;
    private String consequence;
    private String featureType;
    private String allele;
    private String aminoAcids;
    private String sift;
    private Polyphen polyphen;
    private String varPep;
    private List<VariantEffect> variantEffects;
}
