package org.alliancegenome.neo4j.entity.node;

import java.util.*;

import org.alliancegenome.neo4j.entity.Neo4jEntity;
import org.neo4j.ogm.annotation.*;

import lombok.*;

@NodeEntity
@Getter
@Setter
public class ExpressionBioEntity extends Neo4jEntity implements Comparable<ExpressionBioEntity> {

	private String primaryKey;
	private String whereExpressedStatement;

	@Relationship(type = "CELLULAR_COMPONENT_RIBBON_TERM")
	private List<GOTerm> ccRibbonTermList = new ArrayList<>();

	@Relationship(type = "CELLULAR_COMPONENT")
	private List<GOTerm> ccTermList = new ArrayList<>();

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
				(new HashSet<>(aoTermList).equals(new HashSet<>(that.aoTermList))) &&
				(new HashSet<>(ccTermList).equals(new HashSet<>(that.ccTermList)));
	}

	@Override
	public int hashCode() {
		return Objects.hash(whereExpressedStatement);
	}

	@Override
	public String toString() {
		return whereExpressedStatement;
	}
}
