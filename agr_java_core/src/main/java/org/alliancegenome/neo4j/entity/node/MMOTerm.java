package org.alliancegenome.neo4j.entity.node;

import com.fasterxml.jackson.annotation.JsonView;
import lombok.Getter;
import lombok.Setter;
import org.alliancegenome.neo4j.view.View;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@NodeEntity
@Getter
@Setter
public class MMOTerm extends Ontology {

    private String primaryKey;
    @JsonView(View.ExpressionView.class)
    private String name;

    @Override
    public String toString() {
        return name + " [" + primaryKey + "]";
    }
}
