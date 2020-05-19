package org.alliancegenome.neo4j.entity.node;

import com.fasterxml.jackson.annotation.JsonView;
import lombok.Getter;
import lombok.Setter;
import org.alliancegenome.neo4j.entity.Neo4jEntity;
import org.alliancegenome.neo4j.view.View;
import org.neo4j.ogm.annotation.NodeEntity;

@NodeEntity(label = "TranscriptLevelConsequence")
@Getter
@Setter
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

    @JsonView({View.API.class})
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

    @Override
    public int compareTo(TranscriptLevelConsequence o) {
        return 0;
    }

}
