package org.alliancegenome.neo4j.entity.node;

import com.fasterxml.jackson.annotation.JsonView;
import lombok.Getter;
import lombok.Setter;
import org.alliancegenome.neo4j.entity.Neo4jEntity;
import org.alliancegenome.neo4j.view.View;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.neo4j.ogm.annotation.NodeEntity;

@Getter
@Setter
@NodeEntity
@Schema(name="ParaAlgorithm", description="POJO that represents the Para Algorithm")
public class ParaAlgorithm extends Neo4jEntity implements Comparable<ParaAlgorithm> {

	@JsonView({View.OrthologyMethod.class})
	private String name;

	@Override
	public int compareTo(ParaAlgorithm o) {
		return name.compareTo(o.getName());
	}

	@Override
	public String toString() {
		return name;
	}
}
