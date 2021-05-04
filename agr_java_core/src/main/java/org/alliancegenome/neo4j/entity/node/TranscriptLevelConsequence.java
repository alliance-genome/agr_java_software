package org.alliancegenome.neo4j.entity.node;

import org.alliancegenome.core.helpers.VariantServiceHelper;
import org.alliancegenome.neo4j.entity.Neo4jEntity;
import org.alliancegenome.neo4j.view.View;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.neo4j.ogm.annotation.*;

import com.fasterxml.jackson.annotation.*;

import lombok.*;

@NodeEntity(label = "TranscriptLevelConsequence")
@Getter
@Setter
@Schema(name = "TranscriptLevelConsequence", description = "POJO that represents Transcript Level Consequences")
public class TranscriptLevelConsequence extends Neo4jEntity {

    @JsonView({View.API.class, View.GeneAlleleVariantSequenceAPI.class, View.AlleleVariantSequenceConverterForES.class})
    @JsonProperty("molecularConsequence")
    private String transcriptLevelConsequence;
    
    @JsonView({View.Default.class, View.AlleleVariantSequenceConverterForES.class})
    private String impact;
    
    
    @JsonView({View.API.class, View.AlleleVariantSequenceConverterForES.class})
    private String aminoAcidChange;

    @JsonView({View.API.class})
    private String aminoAcidVariation;

    @JsonView({View.API.class})
    private String aminoAcidReference;

    @JsonView({View.API.class, View.AlleleVariantSequenceConverterForES.class})
    private String codonChange;

    @JsonView({View.API.class})
    private String codonReference;

    @JsonView({View.API.class})
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

    @JsonView({View.Default.class})
    private String polyphenScore;

    @JsonView({View.Default.class, View.AlleleVariantSequenceConverterForES.class})
    private String sequenceFeatureType;

    @Relationship(type = "ASSOCIATION", direction = Relationship.INCOMING)
    private Variant variant;

    @Relationship(type = "ASSOCIATION", direction = Relationship.INCOMING)
    private Transcript transcript;

    @JsonView({View.Default.class})
    private String transcriptName;
    @JsonView({View.Default.class, View.AlleleVariantSequenceConverterForES.class})
    private String transcriptID;
    @JsonView({View.Default.class, View.AlleleVariantSequenceConverterForES.class})
    private String transcriptLocation;

    @JsonView({View.GeneAlleleVariantSequenceAPI.class, View.AlleleVariantSequenceConverterForES.class})
    private Gene associatedGene;

    @JsonView({View.Default.class})
    @JsonProperty("transcriptName")
    public String getTranscriptName() {
        if (StringUtils.isNotEmpty(transcriptName))
            return transcriptName;
        if (transcript == null)
            return "";
        transcriptName = transcript.getName();
        return transcriptName;
    }

    @JsonView({View.Default.class})
    @JsonProperty("transcriptID")
    public String getTranscriptID() {
        if (transcriptID != null && transcriptID.length() > 0)
            return transcriptID;
        if (transcript == null)
            return "";
        transcriptID = transcript.getName();
        return transcriptID;
    }
    
    @JsonProperty("location")
    public void setTranscriptLocation(String name) {
        transcriptLocation = name;
    }

    @JsonView({View.GeneAlleleVariantSequenceAPI.class})
    @JsonProperty("location")
    public String getTranscriptLocation() {
        if (transcriptLocation != null && transcriptLocation.length() > 0)
            return transcriptLocation;
        if (transcript == null)
            return "";
        VariantServiceHelper.populateIntronExonLocation(variant, transcript);
        transcriptLocation = transcript.getIntronExonLocation();
        return transcriptLocation;
    }

    @JsonView({View.GeneAlleleVariantSequenceAPI.class})
    public String getSequenceFeatureType() {
        if (StringUtils.isNotEmpty(sequenceFeatureType))
            return sequenceFeatureType;
        if (transcript == null)
            return "";
        sequenceFeatureType = transcript.getType().getName();
        return sequenceFeatureType;
    }

    public TranscriptLevelConsequence() { }

    
    
    public TranscriptLevelConsequence(String[] header, String[] infos, Species species) {

        /* From MOD VCF File
        0  Allele: GGGG
        1  Consequence: intron_variant
        2  IMPACT: MODIFIER
        3  SYMBOL: cpx
        4  Gene: FB:FBgn0041605
        5  Feature_type: Transcript
        6  Feature: FB:FBtr0078897
        7  BIOTYPE: protein_coding
        8  EXON:
        9  INTRON: 3/6
        10 HGVSc: FB:FBtr0078897.1:c.56-4279_56-4275delinsGGGG
        11 HGVSp:
        12 cDNA_position:
        13 CDS_position:
        14 Protein_position:
        15 Amino_acids:
        16 Codons:
        17 Existing_variation:
        18 DISTANCE:
        19 STRAND: 1
        20 FLAGS: 
        21 Gene_level_consequence:
        22 SYMBOL_SOURCE: intron_variant
        23 HGNC_ID:
        24 GIVEN_REF: CCATT
        25 USED_REF: CCATT
        26 BAM_EDIT:
        27 SOURCE: FB_GFF.refseq.gff.gz
        28 HGVS_OFFSET:
        29 HGVSg: NT_033777.3:g.4290841_4290845delinsGGGG
        30 PolyPhen_prediction:
        31 PolyPhen_score:
        32 SIFT_prediction:
        33 SIFT_score:
        34 Genomic_end_position: 4290845
        35 Genomic_start_position: 4290841
        36 transcript_name:cpx-RG
        37 FB_GFF.refseq.gff.gz:
        */
        
        transcriptLevelConsequence = infos[1];
        impact = infos[2];


        associatedGene = new Gene();
        associatedGene.setSymbol(infos[3]);
        associatedGene.setPrimaryKey(infos[4]);
        associatedGene.setSpecies(species);
        
        transcriptID = infos[6];
        sequenceFeatureType = infos[7];
        

        String location = "";
        if (StringUtils.isNotEmpty(infos[8]))
            location += "Exon " + infos[8];
        if (StringUtils.isNotEmpty(infos[9]))
            location += "Intron " + infos[9];
        
        transcriptLocation = location;
        
        hgvsCodingNomenclature = infos[10];
        hgvsProteinNomenclature = infos[11];
        cdnaStartPosition = infos[12];
        cdsStartPosition = infos[13];
        proteinStartPosition = infos[14];
        aminoAcidChange = infos[15];
        codonChange = infos[16];
        
        /*
        flags = infos[20];
        symbolSource = infos[22];
        hgncId = infos[23];
        givenRef = infos[24];
        usedRef = infos[25];
        bamEdit = infos[26];
        source = infos[27];
        hgvsOffset = infos[28]; 
        
         */
        
        hgvsVEPGeneNomenclature = infos[29];
        polyphenPrediction = infos[30];
        polyphenScore = infos[31];
        
        siftPrediction = infos[32];
        siftScore = infos[33];

        
//          // Human VEP Results
//          // Allele|Consequence|IMPACT|SYMBOL|Gene|Feature_type|Feature|BIOTYPE|EXON|INTRON|
//          // HGVSc|HGVSp|cDNA_position|CDS_position|Protein_position|Amino_acids|Codons|Existing_variation|DISTANCE|STRAND|
//          //FLAGS|SYMBOL_SOURCE|HGNC_ID|REFSEQ_MATCH|SOURCE|REFSEQ_OFFSET|GIVEN_REF|USED_REF|BAM_EDIT|SIFT|
//          //PolyPhen|HGVS_OFFSET|HGVSg
//
//          allele = infos[0];
//          consequence = infos[1];
//          impact = infos[2];
//          symbol = infos[3];
//          gene = infos[4];
//          featureType = infos[5];
//          feature = infos[6];
//          biotype = infos[7];
//          exon = infos[8];
//          intron = infos[9];
//
//          hgvsc = infos[10];
//          hgvsp = infos[11];
//          cdnaPosition = infos[12];
//          cdsPosition = infos[13];
//          proteinPosition = infos[14];
//          aminoAcids = infos[15];
//          codons = infos[16];
//          existingVariation = infos[17];
//          distance = infos[18];
//          strand = infos[19];
//
//          flags = infos[20];
//          symbolSource = infos[21];
//          hgncId = infos[22];
//          refseqMatch = infos[23];
//          source = infos[24];
//          refseqOffset = infos[25];
//          givenRef = infos[26];
//          usedRef = infos[27];
//          bamEdit = infos[28];
//          siftPrediction = infos[29];
//
//          polyphenPrediction = infos[30];
//          hgvsOffset = infos[31];
//          hgvsg = infos[32];

        
    }

}
