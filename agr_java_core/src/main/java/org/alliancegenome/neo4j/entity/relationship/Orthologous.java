package org.alliancegenome.neo4j.entity.relationship;

import org.alliancegenome.neo4j.entity.Neo4jEntity;
import org.alliancegenome.neo4j.entity.node.Gene;
import org.alliancegenome.neo4j.view.PublicView;
import org.neo4j.ogm.annotation.EndNode;
import org.neo4j.ogm.annotation.RelationshipEntity;
import org.neo4j.ogm.annotation.StartNode;

import com.fasterxml.jackson.annotation.JsonView;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@RelationshipEntity(type = "ORTHOLOGOUS")
public class Orthologous extends Neo4jEntity {

	@JsonView(PublicView.Homology.class)
	@StartNode private Gene gene1;
	@JsonView(PublicView.Homology.class)
	@EndNode private Gene gene2;

	private String primaryKey;
	@JsonView(PublicView.Homology.class) private String isBestRevScore;
	@JsonView(PublicView.Homology.class) private String isBestScore;
	private String confidence;
	private boolean moderateFilter;
	private boolean strictFilter;

}
