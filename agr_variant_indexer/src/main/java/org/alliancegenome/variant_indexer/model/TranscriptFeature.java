package org.alliancegenome.variant_indexer.model;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
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
