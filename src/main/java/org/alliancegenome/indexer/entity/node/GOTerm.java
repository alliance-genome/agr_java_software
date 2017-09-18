package org.alliancegenome.indexer.entity.node;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import lombok.Getter;
import lombok.Setter;

@NodeEntity
@Getter @Setter
public class GOTerm extends Ontology {

	private String nameKey;
	private String name;
	private String description;
	private String href;
	private String type;
	private String primaryKey;
	private String is_obsolete;
	
	@Relationship(type = "ANNOTATED_TO", direction=Relationship.INCOMING)
	private Set<Gene> genes = new HashSet<Gene>();
	
	@Relationship(type = "ALSO_KNOWN_AS")
	private Set<Synonym> synonyms = new HashSet<Synonym>();

    @Relationship(type = "CROSS_REFERENCE")
    private List<CrossReference> crossReferences;

}
