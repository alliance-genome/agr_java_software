package org.alliancegenome.api.entity;

import com.fasterxml.jackson.annotation.JsonView;
import lombok.Getter;
import lombok.Setter;

import org.alliancegenome.es.index.site.document.SearchableItemDocument;
import org.alliancegenome.neo4j.entity.node.Allele;
import org.alliancegenome.neo4j.entity.node.TranscriptLevelConsequence;
import org.alliancegenome.neo4j.entity.node.Variant;
import org.alliancegenome.neo4j.view.View;

/**
 * A flattened version of an Allele entity for presentational purposes.
 * Allele -> multiple Variants -> multiple TranscriptLevelConsequence
 */
@Setter
@Getter
public class AlleleVariantSequence extends SearchableItemDocument {

    @JsonView({View.Default.class,View.AlleleVariantSequenceConverterForES.class})
    private Allele allele;
    @JsonView({View.Default.class, View.AlleleVariantSequenceConverterForES.class})
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
        if (consequence != null && allele.getGene() != null)
            consequence.setAssociatedGene(allele.getGene());
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(allele.getSymbolText());
        if (variant != null) {
            builder.append(" : ");
            builder.append(variant.getHgvsNomenclature());
        }
        if (consequence != null) {
            builder.append(" : ");
            builder.append(consequence.getTranscript().getName());
        }
        return builder.toString();
    }
}
