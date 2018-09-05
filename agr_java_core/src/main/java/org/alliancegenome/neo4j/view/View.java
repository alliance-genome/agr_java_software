package org.alliancegenome.neo4j.view;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.alliancegenome.neo4j.entity.node.OrthoAlgorithm;

import java.util.List;

public class View {
    public static class OrthologyView {

        @JsonProperty("geneID")
        private String primaryKey;

        @JsonProperty("species")
        private String speciesName;

        /*
        @JsonProperty("bestReverse")
        private boolean isBestRevScore;

        @JsonProperty("best")
        private boolean isBestScore;

        @JsonProperty("species")
        private String gene1SpeciesName;

        @JsonProperty("homologSpecies")
        private String gene2SpeciesName;

        @JsonProperty("geneSymbol")
        private String gene1Symbol;

        @JsonProperty("homologGeneSymbol")
        private String gene2Symbol;

        @JsonProperty("geneID")
        private String gene1AgrPrimaryId;

        @JsonProperty("homologGeneID")
        private String gene2AgrPrimaryId;
*/

    }

    public static class InteractionView {
    }

    public static class OrthologyMethodView {

        @JsonProperty("methods")
        private List<OrthoAlgorithm> results;

    }
}
