package org.alliancegenome.neo4j.entity.node;

import java.util.ArrayList;
import java.util.List;

import org.alliancegenome.neo4j.entity.Neo4jEntity;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import lombok.Getter;
import lombok.Setter;

@NodeEntity
@Getter
@Setter
@Schema(name="HTPDataset", description="POJO that represents a High Throughput Dataset")
public class HTPDataset extends Neo4jEntity {

	private String crossRefCompleteUrl;
	private String dataProvider;
	private String primaryKey;
	private String summary;
	private String title;


	@Relationship(type = "CROSS_REFERENCE")
	protected List<CrossReference> crossReferences = new ArrayList<>();

}
