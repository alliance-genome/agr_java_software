package org.alliancegenome.indexer.entity;

import java.util.Date;
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

	private String primaryKey;
	private String taxonId;
	private String geneLiterature;
	private String geneLiteratureUrl;
	private String geneSynopsis;
	private String geneSynopsisUrl;
	private String dataProvider;
	private String name;
	private Date dataProduced;
	private String description;
	private String symbol;
	
	@Relationship(type = "ALSO_KNOWN_AS")
	private Set<Synonym> synonyms = new HashSet<>();
	
	@Relationship(type = "ANNOTATED_TO")
	private SoTerm soTerm;
	
	
	@Relationship(type = "ANNOTATED_TO")
	private Set<GoTerm> goTerms = new HashSet<>();

}
