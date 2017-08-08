package org.alliancegenome.indexer.entity;

import java.util.HashSet;
import java.util.Set;

import org.neo4j.ogm.annotation.GraphId;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import lombok.Data;
import lombok.ToString;

@NodeEntity
@Data
@ToString(includeFieldNames=true)
public class Gene extends Entity {
	@GraphId
	private Long id;
	private String primaryKey;
	private String taxonId;
	private String geneLiteratureUrl;
	private String geneSynopsisUrl;
	private String dataProvider;
	private String name;
	private String description;
	private String symbol;
	
	@Relationship(type = "ALSO_KNOWN_AS", direction = "OUTGOING")
	private Set<Synonym> synonyms = new HashSet<>();

}
