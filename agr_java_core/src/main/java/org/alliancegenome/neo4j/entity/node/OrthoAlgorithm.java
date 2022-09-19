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
@Schema(name="OrthoAlgorithm", description="POJO that represents the Ortho Algorithm")
public class OrthoAlgorithm extends Neo4jEntity implements Comparable<OrthoAlgorithm> {

	@JsonView({View.OrthologyMethod.class})
	private String name;

	@Override
	public int compareTo(OrthoAlgorithm o) {
		return name.compareTo(o.getName());
	}

	@Override
	public String toString() {
		return name;
	}
}
