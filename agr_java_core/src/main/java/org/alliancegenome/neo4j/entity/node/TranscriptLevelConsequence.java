package org.alliancegenome.neo4j.entity.node;

import org.alliancegenome.api.service.VariantService;
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
    
    
    @JsonView({View.API.class})
    private String aminoAcidChange;

    @JsonView({View.API.class})
    private String aminoAcidVariation;

    @JsonView({View.API.class})
    private String aminoAcidReference;

    @JsonView({View.API.class, View.AlleleVariantSequenceConverterForES.class})
    private String codonChange;

    @JsonView({View.API.class})
    private String codonReference;

    @JsonView({View.API.class, View.AlleleVariantSequenceConverterForES.class})
    private String codonVariation;



    @JsonView({View.API.class})
    private String cdsStartPosition;

    @JsonView({View.API.class})
    private String cdsEndPosition;

    @JsonView({View.API.class})
    private String cdnaStartPosition;

    @JsonView({View.API.class})
    private String cdnaEndPosition;

    @JsonView({View.API.class})
    private String proteinStartPosition;

    @JsonView({View.API.class})
    private String proteinEndPosition;

    @JsonView({View.API.class})
    private String hgvsProteinNomenclature;

    @JsonView({View.API.class})
    private String hgvsCodingNomenclature;

    @JsonView({View.API.class})
    private String hgvsVEPGeneNomenclature;



    @JsonView({View.Default.class})
    private String siftPrediction;

    @JsonView({View.Default.class})
    private String polyphenPrediction;

    @JsonView({View.Default.class})
    private String siftScore;

    @JsonView({View.Default.class})
    private String polyphenScore;

    @JsonView({View.Default.class})
    private String sequenceFeatureType;

    @Relationship(type = "ASSOCIATION", direction = Relationship.INCOMING)
    private Variant variant;

    @Relationship(type = "ASSOCIATION", direction = Relationship.INCOMING)
    private Transcript transcript;

    @JsonView({View.Default.class})
    private String transcriptName;
    @JsonView({View.Default.class})
    private String transcriptID;
    @JsonView({View.Default.class})
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

    @JsonView({View.GeneAlleleVariantSequenceAPI.class, View.AlleleVariantSequenceConverterForES.class})
    @JsonProperty("location")
    public String getTranscriptLocation() {
        if (transcriptLocation != null && transcriptLocation.length() > 0)
            return transcriptLocation;
        if (transcript == null)
            return "";
        VariantService service = new VariantService();
        service.populateIntronExonLocation(variant, transcript);
        transcriptLocation = transcript.getIntronExonLocation();
        return transcriptLocation;
    }

    @JsonView({View.GeneAlleleVariantSequenceAPI.class, View.AlleleVariantSequenceConverterForES.class})
    public String getSequenceFeatureType() {
        if (StringUtils.isNotEmpty(sequenceFeatureType))
            return sequenceFeatureType;
        if (transcript == null)
            return "";
        sequenceFeatureType = transcript.getType().getName();
        return sequenceFeatureType;
    }

    public TranscriptLevelConsequence() { }

    
    
    
    
    
//  private String allele;
//  private String consequence; -> transcriptLevelConsequence;
//  private String symbol;
//  private String gene;
//  private String featureType;
//  private String feature;
//  private String biotype;
//  private String exon;
//  private String intron;
//  private String hgvsc;
//  private String hgvsp;
//  private String hgvsg;
//  
//  private String cdnaPosition;
//  private String cdsPosition;
//  private String proteinPosition;
//  private String aminoAcids;
//  private String codons;
//  private String existingVariation;
//  private String distance;
//  private String strand;
//  private String flags;
//  private String symbolSource;
//  private String hgncId;
//  private String source;
//  private String refseqMatch;
//  private String refseqOffset;
//  private String givenRef;
//  private String usedRef;
//  private String bamEdit;
//
//  private String hgvsOffset;
//  private String genomicStart;
//  private String genomicEnd;
//
//  private String referenceSequence;
//  private String variantSequence;
//  

    
    public TranscriptLevelConsequence(String[] header, String[] infos) {

        // Mod VEP
        //  Allele|Consequence|IMPACT|SYMBOL|Gene|Feature_type|Feature|BIOTYPE|EXON|INTRON
        // |HGVSc|HGVSp|cDNA_position|CDS_position|Protein_position|Amino_acids|Codons|Existing_variation|DISTANCE|STRAND|
        //  FLAGS|SYMBOL_SOURCE|HGNC_ID|GIVEN_REF|USED_REF|BAM_EDIT|SOURCE|HGVS_OFFSET|HGVSg|
        //  PolyPhen_prediction|PolyPhen_score|SIFT_prediction|SIFT_score|Genomic_end_position|Genomic_start_position

        transcriptLevelConsequence = infos[1];
        impact = infos[2];


        associatedGene = new Gene();
        associatedGene.setSymbol(infos[3]);
        associatedGene.setPrimaryKey(infos[4]);
        
        sequenceFeatureType = infos[7];
        transcriptID = infos[6];

        String location = "";
        if (StringUtils.isNotEmpty(infos[8]))
            location += "Exon " + infos[8];
        if (StringUtils.isNotEmpty(infos[9]))
            location += "Intron " + infos[9];
        
        transcriptLocation = location;
        
        /*  biotype = infos[7];
         */
        
        cdnaStartPosition = infos[12];
        cdsStartPosition = infos[13];
        hgvsCodingNomenclature = infos[10];
        hgvsProteinNomenclature = infos[11];
        proteinStartPosition = infos[14];
        aminoAcidChange = infos[15];
        codonChange = infos[16];
        polyphenPrediction = infos[30];
        hgvsVEPGeneNomenclature = infos[28];
        siftPrediction = infos[29];
        siftScore = infos[32];


        //  c.setCodonReference(infos[26]); // need to verify

        //  variant.setGenomicVariantSequence(transcriptFeature.getAllele());
        //  genomicEnd = infos[33];
        //    genomicStart = infos[34];

        /*  existingVariation = infos[17];
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

        hgvsOffset = infos[31];*/
        
        
        
        
        
//      if (header.length == 33) {
//
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
//
//      } else if (header.length == 37) {
//          // Mod VEP
//          //  Allele|Consequence|IMPACT|SYMBOL|Gene|Feature_type|Feature|BIOTYPE|EXON|INTRON
//          // |HGVSc|HGVSp|cDNA_position|CDS_position|Protein_position|Amino_acids|Codons|Existing_variation|DISTANCE|STRAND|
//          //  FLAGS|SYMBOL_SOURCE|HGNC_ID|GIVEN_REF|USED_REF|BAM_EDIT|SOURCE|HGVS_OFFSET|HGVSg|
//          //  PolyPhen_prediction|PolyPhen_score|SIFT_prediction|SIFT_score|Genomic_end_position|Genomic_start_position            // Allele|Consequence|IMPACT|SYMBOL|Gene|Feature_type|Feature|BIOTYPE|EXON|INTRON|
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
//          givenRef = infos[23];
//          usedRef = infos[24];
//          bamEdit = infos[25];
//
//          source = infos[26];
//          hgvsOffset = infos[27];
//          hgvsg = infos[28];
//          polyphenPrediction = infos[29];
//          polyphenScore = infos[30];
//          siftPrediction = infos[31];
//          siftScore = infos[32];
//          genomicEnd = infos[33];
//          genomicStart = infos[34];
//      } else {
//          log.error("Unknown Header Len: " + header.length);
//          log.error(Arrays.asList(infos));
//      }
        
    }

}
