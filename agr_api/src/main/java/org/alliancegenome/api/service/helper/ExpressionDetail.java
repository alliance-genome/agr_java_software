package org.alliancegenome.api.service.helper;

import java.util.List;
import java.util.TreeSet;

import org.alliancegenome.neo4j.entity.node.CrossReference;
import org.alliancegenome.neo4j.entity.node.Gene;
import org.alliancegenome.neo4j.entity.node.MMOTerm;
import org.alliancegenome.neo4j.entity.node.Publication;
import org.alliancegenome.neo4j.entity.node.Stage;
import org.alliancegenome.neo4j.view.View;

import com.fasterxml.jackson.annotation.JsonView;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ExpressionDetail {

    @JsonView(View.ExpressionAPI.class)
    private Gene gene;
    @JsonView(View.ExpressionAPI.class)
    private String termName;
    @JsonView(View.ExpressionAPI.class)
    private Stage stage;
    @JsonView(View.ExpressionAPI.class)
    private MMOTerm assay;
    @JsonView(View.ExpressionAPI.class)
    private TreeSet<Publication> publications;
    @JsonView(View.ExpressionAPI.class)
    private String dataProvider;
    @JsonView(View.ExpressionAPI.class)
    private List<CrossReference> crossReferences;
}
