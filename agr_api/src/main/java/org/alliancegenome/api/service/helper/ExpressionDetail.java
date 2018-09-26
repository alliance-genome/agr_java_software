package org.alliancegenome.api.service.helper;

import com.fasterxml.jackson.annotation.JsonView;
import lombok.Getter;
import lombok.Setter;
import org.alliancegenome.neo4j.entity.node.*;
import org.alliancegenome.neo4j.view.View;

import java.util.List;

@Setter
@Getter
public class ExpressionDetail {

    @JsonView(View.ExpressionView.class)
    private Gene gene;
    @JsonView(View.ExpressionView.class)
    private String termName;
    @JsonView(View.ExpressionView.class)
    private UBERONTerm stage;
    @JsonView(View.ExpressionView.class)
    private MMOTerm assay;
    @JsonView(View.ExpressionView.class)
    private List<Publication> publications;
    @JsonView(View.ExpressionView.class)
    private String dataProvider;
}
