package org.alliancegenome.neo4j.entity.node;

import lombok.Getter;
import lombok.Setter;
import org.alliancegenome.neo4j.entity.Neo4jEntity;
import org.neo4j.ogm.annotation.NodeEntity;

@Getter
@Setter
@NodeEntity
public class OrthoAlgorithm extends Neo4jEntity implements Comparable<OrthoAlgorithm> {

    private String name;

    @Override
    public int compareTo(OrthoAlgorithm o) {
        return name.compareTo(o.getName());
    }
}
