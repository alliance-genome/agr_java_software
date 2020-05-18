package org.alliancegenome.core;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
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
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Setter
@Getter
@Schema(name="ExpressionDetail", description="POJO that represents Expression Detail")
public class ExpressionDetail implements Comparable, Serializable {

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
    private List<CrossReference> crossReferences;

    @JsonView(View.Expression.class)
    private List<String> termIDs = new ArrayList<>(6);

    @JsonView(View.Expression.class)
    private List<String> uberonTermIDs = new ArrayList<>(6);

    @JsonView(View.Expression.class)
    private List<String> goTermIDs = new ArrayList<>(6);

    @JsonView(View.Expression.class)
    private String stageTermID;

    public void addTermIDs(Collection<String> ids) {
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
