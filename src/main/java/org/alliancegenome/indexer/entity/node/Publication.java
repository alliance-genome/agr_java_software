package org.alliancegenome.indexer.entity.node;

import lombok.Getter;
import lombok.Setter;

import org.alliancegenome.indexer.entity.Neo4jEntity;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import java.util.List;

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
