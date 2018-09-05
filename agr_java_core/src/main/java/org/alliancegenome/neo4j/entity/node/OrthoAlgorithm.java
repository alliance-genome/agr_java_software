package org.alliancegenome.neo4j.entity.node;

import com.fasterxml.jackson.annotation.JsonView;
import lombok.Getter;
import lombok.Setter;
import org.alliancegenome.neo4j.entity.Neo4jEntity;
import org.alliancegenome.neo4j.view.View;
import org.neo4j.ogm.annotation.NodeEntity;

@Getter
@Setter
@NodeEntity
public class OrthoAlgorithm extends Neo4jEntity implements Comparable<OrthoAlgorithm> {

    @JsonView({View.OrthologyMethodView.class})
    private String name;

    @Override
    public int compareTo(OrthoAlgorithm o) {
        return name.compareTo(o.getName());
    }
}
