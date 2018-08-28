package org.alliancegenome.neo4j.entity.node;

import lombok.Getter;
import lombok.Setter;
import org.alliancegenome.neo4j.entity.Neo4jEntity;
import org.neo4j.ogm.annotation.NodeEntity;

@NodeEntity
@Getter
@Setter
public class ExpressionBioEntity extends Neo4jEntity implements Comparable<ExpressionBioEntity> {

    private String primaryKey;
    private String whereExpressedStatement;

    @Override
    public int compareTo(ExpressionBioEntity o) {
        return 0;
    }
}
