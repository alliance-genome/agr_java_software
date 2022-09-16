package org.alliancegenome.neo4j.entity.node;

import org.alliancegenome.neo4j.entity.Neo4jEntity;
import org.alliancegenome.neo4j.view.View;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.neo4j.ogm.annotation.NodeEntity;

import com.fasterxml.jackson.annotation.JsonView;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@NodeEntity
@Schema(name = "ExperimentalCondition", description = "POJO that represents ExperimentalCondition nodes")
public class ExperimentalCondition extends Neo4jEntity {

	@JsonView({View.API.class})
	private String primaryKey;

	@JsonView({View.API.class})
	private String conditionStatement;

	@JsonView({View.API.class})
	private ZECOTerm term;

	@Override
	public String toString() {
		return conditionStatement;
	}
}
