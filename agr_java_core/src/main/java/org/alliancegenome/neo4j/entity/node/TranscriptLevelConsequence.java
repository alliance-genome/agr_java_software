package org.alliancegenome.neo4j.entity.node;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.alliancegenome.core.helpers.VariantServiceHelper;
import org.alliancegenome.neo4j.entity.Neo4jEntity;
import org.alliancegenome.neo4j.view.View;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Log4j2
@NodeEntity(label = "TranscriptLevelConsequence")
@Getter
@Setter
@Schema(name = "TranscriptLevelConsequence", description = "POJO that represents Transcript Level Consequences")
public class TranscriptLevelConsequence extends Neo4jEntity {

    @JsonView({View.API.class, View.GeneAlleleVariantSequenceAPI.class, View.AlleleVariantSequenceConverterForES.class})
    @JsonIgnore
    private String transcriptLevelConsequence;

    @JsonView({View.Default.class, View.AlleleVariantSequenceConverterForES.class})
    private List<String> transcriptLevelConsequences = new ArrayList<>();

    @JsonView({View.Default.class, View.AlleleVariantSequenceConverterForES.class})
    private String impact;

    @JsonView({View.API.class, View.AlleleVariantSequenceConverterForES.class})
    private String aminoAcidChange;

    @JsonView({View.API.class, View.AlleleVariantSequenceConverterForES.class})
    private String aminoAcidVariation;

    @JsonView({View.API.class, View.AlleleVariantSequenceConverterForES.class})
    private String aminoAcidReference;

    @JsonView({View.API.class, View.AlleleVariantSequenceConverterForES.class})
    private String codonChange;

    @JsonView({View.API.class, View.AlleleVariantSequenceConverterForES.class})
    private String codonReference;

    @JsonView({View.API.class, View.AlleleVariantSequenceConverterForES.class})
    private String codonVariation;

    @JsonView({View.API.class, View.AlleleVariantSequenceConverterForES.class})
    private String cdsStartPosition;

    @JsonView({View.API.class})
    private String cdsEndPosition;

    @JsonView({View.API.class, View.AlleleVariantSequenceConverterForES.class})
    private String cdnaStartPosition;

    @JsonView({View.API.class})
    private String cdnaEndPosition;

    @JsonView({View.API.class, View.AlleleVariantSequenceConverterForES.class})
    private String proteinStartPosition;

    @JsonView({View.API.class})
    private String proteinEndPosition;

    @JsonView({View.API.class, View.AlleleVariantSequenceConverterForES.class})
    private String hgvsProteinNomenclature;

    @JsonView({View.API.class, View.AlleleVariantSequenceConverterForES.class})
    private String hgvsCodingNomenclature;

    @JsonView({View.API.class, View.AlleleVariantSequenceConverterForES.class})
    private String hgvsVEPGeneNomenclature;


    @JsonView({View.Default.class, View.AlleleVariantSequenceConverterForES.class})
    private String siftPrediction;

    @JsonView({View.Default.class, View.AlleleVariantSequenceConverterForES.class})
    private String polyphenPrediction;

    @JsonView({View.Default.class, View.AlleleVariantSequenceConverterForES.class})
    private String siftScore;

    @JsonView({View.Default.class, View.AlleleVariantSequenceConverterForES.class})
    private String polyphenScore;

    @JsonView({View.Default.class, View.AlleleVariantSequenceConverterForES.class})
    private String sequenceFeatureType;

    @Relationship(type = "ASSOCIATION", direction = Relationship.INCOMING)
    private Variant variant;

    @Relationship(type = "ASSOCIATION", direction = Relationship.INCOMING)
    private Transcript transcript;

    //@JsonView({View.Default.class, View.AlleleVariantSequenceConverterForES.class})
    //private String transcriptName;
    //@JsonView({View.Default.class, View.AlleleVariantSequenceConverterForES.class})
    //private String transcriptID;
    @JsonView({View.Default.class, View.AlleleVariantSequenceConverterForES.class})
    private String location;

    @JsonView({View.GeneAlleleVariantSequenceAPI.class, View.AlleleVariantSequenceConverterForES.class})
    private Gene associatedGene;

    public void setLocation(String name) {
        location = name;
    }

    @JsonView({View.GeneAlleleVariantSequenceAPI.class, View.AlleleVariantSequenceConverterForES.class})
    public String getLocation() {
        if (StringUtils.isNotEmpty(location))
            return location;
        if (transcript == null || variant == null)
            return "";
        VariantServiceHelper.populateIntronExonLocation(variant, transcript);
        location = transcript.getIntronExonLocation();
        return location;
    }

    @JsonView({View.GeneAlleleVariantSequenceAPI.class, View.AlleleVariantSequenceConverterForES.class})
    public String getSequenceFeatureType() {
        if (StringUtils.isNotEmpty(sequenceFeatureType))
            return sequenceFeatureType;
        if (transcript == null)
            return "";
        if (transcript.getType() != null)
            sequenceFeatureType = transcript.getType().getName();
        return sequenceFeatureType;
    }

    public void setSequenceFeatureType(String sequenceFeatureType) {
        this.sequenceFeatureType = sequenceFeatureType;
    }

    public TranscriptLevelConsequence() {
    }

    @JsonView({View.API.class, View.AlleleVariantSequenceConverterForES.class})
    private String geneLevelConsequence;

    private String hgncId;

    // only used for Neo
    public String getTranscriptLevelConsequence() {
        return transcriptLevelConsequence;
    }

    // only used for Neo
    public void setTranscriptLevelConsequence(String transcriptLevelConsequence) {
        this.transcriptLevelConsequence = transcriptLevelConsequence;
        if (transcriptLevelConsequence != null) {
            /// TODO make loader do this work
            transcriptLevelConsequences.addAll(Arrays.asList(transcriptLevelConsequence.split(",")));
        }
    }

    @JsonProperty("molecularConsequence")
    public List<String> getTranscriptLevelConsequences() {
        if (CollectionUtils.isNotEmpty(transcriptLevelConsequences))
            return transcriptLevelConsequences;
        if (transcriptLevelConsequence != null)
            /// TODO make loader do this work
            return Arrays.asList(transcriptLevelConsequence.split(","));
        return null;
    }

    @JsonProperty("molecularConsequence")
    public void setTranscriptLevelConsequences(List<String> transcriptLevelConsequences) {
        this.transcriptLevelConsequences = transcriptLevelConsequences;
    }

    public TranscriptLevelConsequence(String[] header, String[] infos) {

        // VCF Header from the file
        /*
         * 0 Allele
         * 1 Consequence
         * 2 IMPACT
         * 3 SYMBOL
         * 4 Gene
         * 5 Feature_type
         * 6 Feature
         * 7 BIOTYPE
         * 8 EXON
         * 9 INTRON
         * 10 HGVSc
         * 11 HGVSp
         * 12 cDNA_position
         * 13 CDS_position
         * 14 Protein_position
         * 15 Amino_acids
         * 16 Codons
         * 17 Existing_variation
         * 18 DISTANCE
         * 19 STRAND
         * 20 FLAGS
         * 21 Gene_level_consequence
         * 22 SYMBOL_SOURCE
         * 23 HGNC_ID
         * 24 GIVEN_REF
         * 25 USED_REF
         * 26 BAM_EDIT
         * 27 SOURCE
         * 28 HGVS_OFFSET
         * 29 HGVSg
         * 30 PolyPhen_prediction
         * 31 PolyPhen_score
         * 32 SIFT_prediction
         * 33 SIFT_score
         * 34 transcript_name
         * 35 Genomic_end_position
         * 36 Genomic_start_position
         * 37 HUMAN_GFF.refseq.gff.gz
         * */


        if (infos.length == 38) {

            transcriptLevelConsequences = Arrays.asList(infos[1].split("&"));
            impact = infos[2];

            if(infos[3].length() > 0 && infos[4].length() > 0) {
                associatedGene = new Gene();
                associatedGene.setSymbol(infos[3]);
                associatedGene.setPrimaryKey(infos[4]);
            }

            // Not sure about field 5?

            if(infos[6].length() > 0) {
                transcript = new Transcript();
                transcript.setName(infos[6]);
                transcript.setPrimaryKey(infos[6]);
            }
            
            sequenceFeatureType = infos[7];

            location = "";
            if (infos[8].length() > 0)
                location += "Exon " + infos[8];
            if (infos[9].length() > 0)
                location += "Intron " + infos[9];

            hgvsCodingNomenclature = infos[10];
            hgvsProteinNomenclature = infos[11];
            cdnaStartPosition = infos[12];
            cdsStartPosition = infos[13];
            proteinStartPosition = infos[14];
            aminoAcidChange = infos[15];

            if (aminoAcidChange.length() > 0) {
                aminoAcidReference = aminoAcidChange;
                aminoAcidVariation = aminoAcidChange;
            }
            codonChange = infos[16];
            if (codonChange.length() > 0) {
                String[] codonToken = codonChange.split("/");
                if (codonToken.length == 2) {
                    codonReference = codonToken[0];
                    codonVariation = codonToken[1];
                }
            }

            /* 17 - 20? */

            geneLevelConsequence = infos[21];

            // symbolSource = infos[22];

            hgncId = infos[23];
            if (hgncId.length() > 0)
                associatedGene.setPrimaryKey(hgncId);

            //givenRef = infos[24];
            //usedRef = infos[25];
            //bamEdit = infos[26];
            //source = infos[27];
            //hgvsOffset = infos[28]; 

            hgvsVEPGeneNomenclature = infos[29];
            polyphenPrediction = infos[30];
            polyphenScore = infos[31];

            siftPrediction = infos[32];
            siftScore = infos[33];

//            hgvsVEPGeneNomenclature = infos[34];

            // 35 and 36

        } else {
            // FAIL
            log.error("File Headers and Data DO NOT MATCH this code please check the input VCF files");
            log.error("This code WILL NOT work correctly until this issue is fixed");
            log.error("Header Len: " + header.length + ", Info Len: " + infos.length);
            try {
                for (int i = 0; i < header.length; i++) {
                    log.error("Header: " + header[i] + ": \"" + infos[i] + "\"");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            System.exit(-1);
        }

    }

}
