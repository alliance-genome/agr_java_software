package org.alliancegenome.neo4j.entity.node;

import lombok.Getter;
import lombok.Setter;
import org.alliancegenome.neo4j.entity.Neo4jEntity;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import java.util.List;

@NodeEntity
@Getter
@Setter
public class ExpressionBioEntity extends Neo4jEntity implements Comparable<ExpressionBioEntity> {

    private String primaryKey;
    private String whereExpressedStatement;

    @Relationship(type = "CELLULAR_COMPONENT")
    private GOTerm goTerm ;

    @Relationship(type = "ANATOMICAL_RIBBON_TERM")
    private List<UBERONTerm> aoTermList;

/*
    @Relationship(type = "ANATOMICAL_STRUCTURE")
    private GOTerm anatomy ;
*/

    @Override
    public int compareTo(ExpressionBioEntity o) {
        return 0;
    }
}
