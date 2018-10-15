package org.alliancegenome.neo4j.entity.node;

import lombok.Getter;
import lombok.Setter;
import org.alliancegenome.neo4j.entity.Neo4jEntity;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@NodeEntity
@Getter
@Setter
public class ExpressionBioEntity extends Neo4jEntity implements Comparable<ExpressionBioEntity> {

    private String primaryKey;
    private String whereExpressedStatement;

    @Relationship(type = "CELLULAR_COMPONENT")
    private GOTerm goTerm;

    @Relationship(type = "CELLULAR_COMPONENT_RIBBON_TERM")
    private List<UBERONTerm> cellularTermList = new ArrayList<>();

    @Relationship(type = "ANATOMICAL_RIBBON_TERM")
    private List<UBERONTerm> aoTermList = new ArrayList<>();

    @Override
    public int compareTo(ExpressionBioEntity o) {
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExpressionBioEntity that = (ExpressionBioEntity) o;
        return Objects.equals(whereExpressedStatement, that.whereExpressedStatement) &&
                Objects.equals(goTerm, that.goTerm) &&
                Objects.equals(aoTermList, that.aoTermList);
    }

    @Override
    public int hashCode() {

        return Objects.hash(whereExpressedStatement, goTerm, aoTermList);
    }
}
