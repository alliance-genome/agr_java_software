package org.alliancegenome.es.variant.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Getter
@Setter
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
    private String siftPrediction;
    private String siftScore;
    private String polyphenPrediction;
    private String polyphenScore;
    private String hgvsOffset;
    private String genomicStart;
    private String genomicEnd;

    private String referenceSequence;
    private String variantSequence;

    private static final Pattern regex = Pattern.compile("(.*)(\\()(.*)(\\))");

    public TranscriptFeature(String[] header, String[] infos) {


        if (header.length == 33) {

            // Human VEP Results
            // Allele|Consequence|IMPACT|SYMBOL|Gene|Feature_type|Feature|BIOTYPE|EXON|INTRON|
            // HGVSc|HGVSp|cDNA_position|CDS_position|Protein_position|Amino_acids|Codons|Existing_variation|DISTANCE|STRAND|
            //FLAGS|SYMBOL_SOURCE|HGNC_ID|REFSEQ_MATCH|SOURCE|REFSEQ_OFFSET|GIVEN_REF|USED_REF|BAM_EDIT|SIFT|
            //PolyPhen|HGVS_OFFSET|HGVSg

            allele = infos[0];
            consequence = infos[1];
            impact = infos[2];
            symbol = infos[3];
            gene = infos[4];
            featureType = infos[5];
            feature = infos[6];
            biotype = infos[7];
            exon = infos[8];
            intron = infos[9];

            hgvsc = infos[10];
            hgvsp = infos[11];
            cdnaPosition = infos[12];
            cdsPosition = infos[13];
            proteinPosition = infos[14];
            aminoAcids = infos[15];
            codons = infos[16];
            existingVariation = infos[17];
            distance = infos[18];
            strand = infos[19];

            flags = infos[20];
            symbolSource = infos[21];
            hgncId = infos[22];
            refseqMatch = infos[23];
            source = infos[24];
            refseqOffset = infos[25];
            givenRef = infos[26];
            usedRef = infos[27];
            bamEdit = infos[28];
            siftPrediction = infos[29];

            polyphenPrediction = infos[30];
            hgvsOffset = infos[31];
            hgvsg = infos[32];

        } else if (header.length == 36) {
            // Mod VEP
            //  Allele|Consequence|IMPACT|SYMBOL|Gene|Feature_type|Feature|BIOTYPE|EXON|INTRON
            // |HGVSc|HGVSp|cDNA_position|CDS_position|Protein_position|Amino_acids|Codons|Existing_variation|DISTANCE|STRAND|
            //  FLAGS|SYMBOL_SOURCE|HGNC_ID|GIVEN_REF|USED_REF|BAM_EDIT|SOURCE|HGVS_OFFSET|HGVSg|
            //  PolyPhen_prediction|PolyPhen_score|SIFT_prediction|SIFT_score|Genomic_end_position|Genomic_start_position            // Allele|Consequence|IMPACT|SYMBOL|Gene|Feature_type|Feature|BIOTYPE|EXON|INTRON|

            allele = infos[0];
            consequence = infos[1];
            impact = infos[2];
            symbol = infos[3];
            gene = infos[4];
            featureType = infos[5];
            feature = infos[6];
            biotype = infos[7];
            exon = infos[8];
            intron = infos[9];

            hgvsc = infos[10];
            hgvsp = infos[11];
            cdnaPosition = infos[12];
            cdsPosition = infos[13];
            proteinPosition = infos[14];
            aminoAcids = infos[15];
            codons = infos[16];
            existingVariation = infos[17];
            distance = infos[18];
            strand = infos[19];

            flags = infos[20];
            symbolSource = infos[21];
            hgncId = infos[22];
            givenRef = infos[23];
            usedRef = infos[24];
            bamEdit = infos[25];

            source = infos[26];
            hgvsOffset = infos[27];
            hgvsg = infos[28];
            polyphenPrediction = infos[29];
            polyphenScore = infos[30];
            siftPrediction = infos[31];
            siftScore = infos[32];
            genomicEnd = infos[33];
            genomicStart = infos[34];
        }
    }
}
