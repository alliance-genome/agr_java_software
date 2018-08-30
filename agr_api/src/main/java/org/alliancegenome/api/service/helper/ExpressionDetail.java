package org.alliancegenome.api.service.helper;

import com.fasterxml.jackson.annotation.JsonView;
import lombok.Getter;
import lombok.Setter;
import org.alliancegenome.neo4j.entity.node.Gene;
import org.alliancegenome.neo4j.entity.node.MMOTerm;
import org.alliancegenome.neo4j.entity.node.Publication;
import org.alliancegenome.neo4j.entity.node.Stage;
import org.alliancegenome.neo4j.view.View;

@Setter
@Getter
public class ExpressionDetail {

    @JsonView(View.ExpressionView.class)
    private Gene gene;
    @JsonView(View.ExpressionView.class)
    private String termName;
    @JsonView(View.ExpressionView.class)
    private Stage stage;
    @JsonView(View.ExpressionView.class)
    private MMOTerm assay;
    @JsonView(View.ExpressionView.class)
    private Publication publication;
}
