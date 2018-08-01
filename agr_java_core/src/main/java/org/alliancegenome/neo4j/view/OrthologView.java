package org.alliancegenome.neo4j.view;

import com.fasterxml.jackson.annotation.JsonView;
import lombok.Getter;
import lombok.Setter;
import org.alliancegenome.neo4j.entity.node.Gene;

import java.util.List;

@Setter
@Getter
public class OrthologView {

    @JsonView(View.OrthologyView.class)
    private Gene gene;
    @JsonView(View.OrthologyView.class)
    private Gene homologGene;

    @JsonView(View.OrthologyView.class)
    private boolean best;
    @JsonView(View.OrthologyView.class)
    private boolean bestReverse;

    @JsonView(View.OrthologyView.class)
    private List<String> predictionMethodsNotCalled;
    @JsonView(View.OrthologyView.class)
    private List<String> predictionMethodsMatched;
    @JsonView(View.OrthologyView.class)
    private List<String> predictionMethodsNotMatched;

    @JsonView(View.OrthologyView.class)
    private Integer methodCount;
    @JsonView(View.OrthologyView.class)
    private Integer totalMethodCount;

    public void calculateCounts() {
        methodCount=predictionMethodsMatched.size();
        totalMethodCount = predictionMethodsMatched.size() + predictionMethodsNotMatched.size();
    }
}
