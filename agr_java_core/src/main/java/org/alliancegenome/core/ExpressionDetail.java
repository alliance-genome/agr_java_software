package org.alliancegenome.core;

import com.fasterxml.jackson.annotation.JsonView;
import lombok.Getter;
import lombok.Setter;
import org.alliancegenome.neo4j.entity.node.*;
import org.alliancegenome.neo4j.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

@Setter
@Getter
public class ExpressionDetail implements Comparable{

    @JsonView(View.Expression.class)
    private Gene gene;
    @JsonView(View.Expression.class)
    private String termName;
    @JsonView(View.Expression.class)
    private Stage stage;
    @JsonView(View.Expression.class)
    private MMOTerm assay;
    @JsonView(View.Expression.class)
    private TreeSet<Publication> publications;
    @JsonView(View.Expression.class)
    private String dataProvider;
    @JsonView(View.Expression.class)
    private CrossReference crossReference;

    private List<String> termIDs = new ArrayList<>(6);

    public void addTermIDs(List<String> ids) {
        termIDs.addAll(ids);
    }

    public void addTermID(String primaryKey) {
        termIDs.add(primaryKey);
    }

    @Override
    public int compareTo(Object o) {
        return 0;
    }
}
