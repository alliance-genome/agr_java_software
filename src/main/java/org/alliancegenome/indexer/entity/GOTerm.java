package org.alliancegenome.indexer.entity;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import lombok.Data;
import lombok.ToString;

@NodeEntity
@Data
@ToString(includeFieldNames=true)
public class GOTerm extends Ontology {

	private String nameKey;
	private String name;
	private String description;
	private String href;
	private String type;
	private String primaryKey;
	
	//@Relationship(type = "ANNOTATED_TO")
	//private List<Gene> genes;
	
	@Relationship(type = "ALSO_KNOWN_AS")
	private Set<Synonym> synonyms = new HashSet<Synonym>();
}
