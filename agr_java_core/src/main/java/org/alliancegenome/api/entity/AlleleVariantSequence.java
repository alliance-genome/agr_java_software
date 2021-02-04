package org.alliancegenome.api.entity;

import org.alliancegenome.neo4j.entity.node.*;
import org.alliancegenome.neo4j.view.View;

import com.fasterxml.jackson.annotation.JsonView;

import lombok.*;

/**
 * A flattened version of an Allele entity for presentational purposes.
 *   Allele -> multiple Variants -> multiple TranscriptLevelConsequence
 */
@Setter
@Getter
public class AlleleVariantSequence {

    @JsonView({View.Default.class})
    private Allele allele;
    @JsonView({View.Default.class})
    private Variant variant;
    @JsonView({View.Default.class})
    private TranscriptLevelConsequence consequence;

    // Used only for deserialization purposes
    public AlleleVariantSequence() {
    }

    public AlleleVariantSequence(Allele allele, Variant variant, TranscriptLevelConsequence consequence) {
        this.allele = allele;
        this.variant = variant;
        this.consequence = consequence;
    }
}
