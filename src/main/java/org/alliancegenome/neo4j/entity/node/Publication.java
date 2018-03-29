package org.alliancegenome.neo4j.entity.node;

import java.util.List;

import org.alliancegenome.neo4j.entity.Neo4jEntity;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import lombok.Getter;
import lombok.Setter;

@NodeEntity
@Getter
@Setter
public class Publication extends Neo4jEntity {

	private String primaryKey;
	private String pubMedId;
	private String pubMedUrl;
	private String pubModId;
	private String pubModUrl;

	@Relationship(type = "ANNOTATED_TO")
	private List<EvidenceCode> evidence;

}
