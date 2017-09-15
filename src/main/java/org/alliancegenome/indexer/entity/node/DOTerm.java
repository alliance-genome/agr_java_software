package org.alliancegenome.indexer.entity.node;

import java.util.List;

import org.alliancegenome.indexer.entity.Neo4jEntity;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import lombok.Getter;
import lombok.Setter;

@NodeEntity
@Getter
@Setter
public class DOTerm extends Neo4jEntity {

	private String doUrl;
	private String doDisplayId;
	private String doId;
	private String doPrefix;
	private String primaryKey;
	private String name;
	private String definition;
	private List<String> defLinks;

	private String nameKey;
	private String is_obsolete;

	private String zfinLink;
	private String humanLink;
	private String rgdLink;
	private String wormbaseLink;
	private String flybaseLink;
	private String mgiLink;

	@Relationship(type = "IS_IMPLICATED_IN", direction = Relationship.INCOMING)
	private List<Gene> genes;

	@Relationship(type = "ASSOCIATION", direction = Relationship.INCOMING)
	private List<DiseaseGeneJoin> diseaseGeneJoins;

	@Relationship(type = "IS_A", direction = Relationship.INCOMING)
	private List<DOTerm> children;

	@Relationship(type = "IS_A")
	private List<DOTerm> parents;

	@Relationship(type = "ALSO_KNOWN_AS")
	private List<Synonym> synonyms;

	@Relationship(type = "ALSO_KNOWN_AS")
	private List<CrossReference> externalIds;

}
