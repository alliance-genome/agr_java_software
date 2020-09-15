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
    private String polyphen;
    private String varPep;
    private List<VariantEffect> variantEffects;
    private String impact;
    private String symbol;
    private String gene;
    private String biotype;
    private String exon;
    private String intron;
    private String HGVSg;
    private String HGVSp;
    private String HGVSc;
    private String cDNAPosition;
    private String CDSPosition;
    private  String proteinPosition;
    private String codon;
    private String existingVariation;
    private String distance;
    private String strand;
    private String flags;
    private String symbolSource;
    private String HGNCId;
    private String source;
    private String HGVSOffset;

}
