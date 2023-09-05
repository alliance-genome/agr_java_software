package org.alliancegenome.neo4j.entity.relationship;

import com.fasterxml.jackson.annotation.JsonView;
import lombok.Getter;
import lombok.Setter;
import org.alliancegenome.neo4j.entity.Neo4jEntity;
import org.alliancegenome.neo4j.entity.node.Gene;
import org.alliancegenome.neo4j.view.OrthologyFilter;
import org.alliancegenome.neo4j.view.View;
import org.neo4j.ogm.annotation.EndNode;
import org.neo4j.ogm.annotation.RelationshipEntity;
import org.neo4j.ogm.annotation.StartNode;

@Getter
@Setter
@RelationshipEntity(type = "PARALOGOUS")
public class Paralogous extends Neo4jEntity {

	@JsonView(View.Homology.class)
	@StartNode
	private Gene gene1;
	@JsonView(View.Homology.class)
	@EndNode
	private Gene gene2;

	private String primaryKey;
	@JsonView(View.Homology.class)
	private String length;
	@JsonView(View.Homology.class)
	private String similarity;
	private String confidence;
	private Integer rank;
	private String identity;

}
