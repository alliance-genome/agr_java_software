package org.alliancegenome.indexer.entity;

import java.util.HashSet;
import java.util.Set;

import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import lombok.Getter;
import lombok.Setter;

@NodeEntity
@Getter @Setter
public class Gene extends Neo4jNode {

	private String primaryKey;
	private String taxonId;
	private String geneLiterature;
	private String geneLiteratureUrl;
	private String geneSynopsis;
	private String geneSynopsisUrl;
	private String dataProvider;
	private String name;

	//private Date dateProduced;
	private String description;
	private String symbol;
	private String geneticEntityExternalUrl;
	
	
	private Entity createdBy;
	private SOTerm sOTerm;
	
	@Relationship(type = "FROM_SPECIES")
	private Species species;

	@Relationship(type = "ALSO_KNOWN_AS")
	private Set<Synonym> synonyms = new HashSet<Synonym>();
	
	@Relationship(type = "ALSO_KNOWN_AS")
	private Set<SecondaryId> secondaryIds = new HashSet<SecondaryId>();
	
	@Relationship(type = "ALSO_KNOWN_AS")
	private Set<ExternalId> externalIds = new HashSet<ExternalId>();

	@Relationship(type = "ANNOTATED_TO")
	private Set<GOTerm> gOTerms = new HashSet<GOTerm>();
	
}
