package org.alliancegenome.variant_indexer.es.model;

import java.lang.reflect.*;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.*;

@Getter @Setter
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class TranscriptFeature {
    private String allele;
    private String consequence;
    private String symbol;
    private String gene;
    private String featureType;
    private String feature;
    private String biotype;
    private String exon;
    private String intron;
    private String hgvsc;
    private String hgvsp;
    private String hgvsg;
    private String impact;
    private String cdnaPosition;
    private String cdsPosition;
    private String proteinPosition;
    private String aminoAcids;
    private String codons;
    private String existingVariation;
    private String distance;
    private String strand;
    private String flags;
    private String symbolSource;
    private String hgncId;
    private String source;
    private String refseqMatch;
    private String refseqOffset;
    private String givenRef;
    private String usedRef;
    private String bamEdit;
    private String sift;
    private String polyphen;
    private String hgvsOffset;
}
