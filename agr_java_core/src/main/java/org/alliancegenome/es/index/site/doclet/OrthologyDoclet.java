package org.alliancegenome.es.index.site.doclet;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonView;
import org.alliancegenome.es.index.ESDoclet;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.alliancegenome.neo4j.view.View;

@Getter @Setter @AllArgsConstructor
public class OrthologyDoclet extends ESDoclet {
    
    private String primaryKey;
    @JsonView(View.OrthologyView.class)
    private Boolean isBestScore;
    @JsonView(View.OrthologyView.class)
    private Boolean isBestRevScore;
    private String confidence;

    private String gene1Species;
    private String gene2Species;

    @JsonView(View.OrthologyView.class)
    private String gene1SpeciesName;
    @JsonView(View.OrthologyView.class)
    private String gene2SpeciesName;

    @JsonView(View.OrthologyView.class)
    private String gene1Symbol;
    @JsonView(View.OrthologyView.class)
    private String gene2Symbol;

    @JsonView(View.OrthologyView.class)
    private String gene1AgrPrimaryId;
    @JsonView(View.OrthologyView.class)
    private String gene2AgrPrimaryId;

    @JsonView(View.OrthologyView.class)
    private List<String> predictionMethodsNotCalled;
    @JsonView(View.OrthologyView.class)
    private List<String> predictionMethodsMatched;
    @JsonView(View.OrthologyView.class)
    private List<String> predictionMethodsNotMatched;

}
