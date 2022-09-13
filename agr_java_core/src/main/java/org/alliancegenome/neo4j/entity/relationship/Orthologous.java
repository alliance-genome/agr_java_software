package org.alliancegenome.neo4j.entity.relationship;

import org.alliancegenome.neo4j.entity.Neo4jEntity;
import org.alliancegenome.neo4j.entity.node.Gene;
import org.alliancegenome.neo4j.view.*;
import org.neo4j.ogm.annotation.*;

import com.fasterxml.jackson.annotation.JsonView;

import lombok.*;

@Getter
@Setter
@RelationshipEntity(type = "ORTHOLOGOUS")
public class Orthologous extends Neo4jEntity {

	@JsonView(View.Orthology.class)
	@StartNode
	private Gene gene1;
	@JsonView(View.Orthology.class)
	@EndNode
	private Gene gene2;

	private String primaryKey;
	@JsonView(View.Orthology.class)
	private String isBestRevScore;
	@JsonView(View.Orthology.class)
	private String isBestScore;
	private String confidence;
	private boolean moderateFilter;
	private boolean strictFilter;

	public boolean hasFilter(OrthologyFilter filter) {
		OrthologyFilter.Stringency stringency = filter.getStringency();
		if (stringency.equals(OrthologyFilter.Stringency.ALL))
			return true;
		if (stringency.equals(OrthologyFilter.Stringency.MODERATE) && moderateFilter)
			return true;
		if (stringency.equals(OrthologyFilter.Stringency.STRINGENT) && strictFilter)
			return true;
		return false;
	}
}
