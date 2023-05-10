package org.alliancegenome.neo4j.entity.node;

import java.util.List;

import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@NodeEntity
public class OrthologyGeneJoin extends Association {

	private String primaryKey;
	private String joinType;
	
	@Relationship(type = "NOT_MATCHED")
	private List<OrthoAlgorithm> notMatched;
	
	@Relationship(type = "MATCHED")
	private List<OrthoAlgorithm> matched;
	
	@Relationship(type = "NOT_CALLED")
	private List<OrthoAlgorithm> notCalled;

	@Relationship(type = "ASSOCIATION", direction =	 Relationship.Direction.INCOMING)
	private Gene gene1;

	@Relationship(type = "ASSOCIATION")
	private Gene gene2;

}
