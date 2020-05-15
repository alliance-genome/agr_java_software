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

    @JsonView({View.Default.class, View.API.class})
    private String aminoAcidChange;

    @JsonView({View.Default.class, View.API.class})
    private String aminoAcidVariation;

    @JsonView({View.Default.class, View.API.class})
    private String aminoAcidReference;

    @JsonView({View.Default.class, View.API.class})
    private String codonChange;

    @JsonView({View.Default.class, View.API.class})
    private String codonReference;

    @JsonView({View.Default.class, View.API.class})
    private String codonVariation;

    @JsonView({View.Default.class, View.API.class})
    private String transcriptLevelConsequence;

    @JsonView({View.Default.class, View.API.class})
    private String cdsStartPosition;

    @JsonView({View.Default.class, View.API.class})
    private String cdsEndPosition;

    @JsonView({View.Default.class, View.API.class})
    private String cdnaStartPosition;

    @JsonView({View.Default.class, View.API.class})
    private String cdnaEndPosition;

    @JsonView({View.Default.class, View.API.class})
    private String proteinStartPosition;

    @JsonView({View.Default.class, View.API.class})
    private String proteinEndPosition;

    @Override
    public int compareTo(TranscriptLevelConsequence o) {
        return 0;
    }

}
