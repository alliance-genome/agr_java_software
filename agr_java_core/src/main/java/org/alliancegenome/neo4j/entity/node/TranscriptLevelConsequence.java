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
public class TranscriptLevelConsequence extends Neo4jEntity implements Comparable<TranscriptLevelConsequence> {

    @JsonView({View.API.class})
    private String aminoAcidChange;

    @JsonView({View.API.class})
    private String aminoAcidVariation;

    @JsonView({View.API.class})
    private String aminoAcidReference;

    @JsonView({View.API.class})
    private String codonChange;

    @JsonView({View.API.class})
    private String codonReference;

    @JsonView({View.API.class})
    private String codonVariation;

    @JsonView({View.API.class, View.GeneAlleleVariantSequenceAPI.class})
    private String transcriptLevelConsequence;

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
    private String impact;

    @JsonView({View.Default.class})
    private String siftPrediction;

    @JsonView({View.Default.class})
    private String polyphenPrediction;

    @JsonView({View.Default.class})
    private String siftScore;

    @JsonView({View.Default.class})
    private String polyphenScore;

    @Relationship(type = "ASSOCIATION", direction = Relationship.INCOMING)
    private Variant variant;

    @Relationship(type = "ASSOCIATION", direction = Relationship.INCOMING)
    private Transcript transcript;

    private String transcriptName;

    @JsonProperty("transcriptName")
    public void setTranscriptName(String name) {
        transcriptName = name;
    }

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

    private String transcriptID;

    @JsonProperty("transcriptID")
    public void setTranscriptID(String name) {
        transcriptID = name;
    }

    @JsonView({View.Default.class})
    @JsonProperty("transcriptID")
    public String getTranscriptID() {
        if (StringUtils.isNotEmpty(transcriptID))
            return transcriptID;
        if (transcript == null)
            return "";
        transcriptID = transcript.getName();
        return transcriptID;
    }

    private String transcriptLocation;

    @JsonProperty("location")
    public void setTranscriptLocation(String name) {
        transcriptLocation = name;
    }

    @JsonView({View.GeneAlleleVariantSequenceAPI.class})
    @JsonProperty("location")
    public String getTranscriptLocation() {
        if (StringUtils.isNotEmpty(transcriptLocation))
            return transcriptLocation;
        if (transcript == null)
            return "";
        VariantService service = new VariantService();
        service.populateIntronExonLocation(variant, transcript);
        transcriptLocation = transcript.getIntronExonLocation();
        return transcriptLocation;
    }

    private String transcriptType;

    @JsonProperty("type")
    public void setTranscriptType(String name) {
        transcriptType = name;
    }

    @JsonView({View.GeneAlleleVariantSequenceAPI.class})
    @JsonProperty("type")
    public String getTranscriptType() {
        if (StringUtils.isNotEmpty(transcriptType))
            return transcriptType;
        if (transcript == null)
            return "";
        transcriptType = transcript.getType().getName();
        return transcriptType;
    }

    @Override
    public int compareTo(TranscriptLevelConsequence o) {
        return 0;
    }

}
