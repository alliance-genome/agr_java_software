package org.alliancegenome.neo4j.entity.node;

import java.util.*;

import org.alliancegenome.neo4j.view.View;
import org.neo4j.ogm.annotation.*;

import com.fasterxml.jackson.annotation.JsonView;

import lombok.*;

@NodeEntity
@Getter @Setter
public class UBERONTerm extends Ontology {

	@JsonView(View.Expression.class)
	private String name;
	private String definition;
	private String href;
	private String type;
	@JsonView(View.Expression.class)
	private String primaryKey;
	private String isObsolete;
	private List<String> subset;

	@Relationship(type = "ANNOTATED_TO", direction=Relationship.INCOMING)
	private Set<Gene> genes = new HashSet<>();
	
	@Relationship(type = "ALSO_KNOWN_AS")
	private Set<Synonym> synonyms = new HashSet<Synonym>();

	@Relationship(type = "CROSS_REFERENCE")
	private List<CrossReference> crossReferences;

	@Relationship(type = "IS_A")
	private Set<UBERONTerm> isAParents = new HashSet<>();

	@Relationship(type = "PART_OF")
	private Set<UBERONTerm> partOfParents = new HashSet<>();

	public Set<UBERONTerm> getParentTerms() {
		Set<UBERONTerm> parentTerms = new HashSet<>();

		isAParents.forEach(parent -> { parentTerms.addAll(parent.getParentTerms());});
		partOfParents.forEach(parent -> {parentTerms.addAll(parent.getParentTerms());});

		parentTerms.add(this);

		return parentTerms;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		UBERONTerm goTerm = (UBERONTerm) o;

		return primaryKey.equals(goTerm.primaryKey);
	}

	@Override
	public int hashCode() {
		return primaryKey.hashCode();
	}

	@Override
	public String toString() { return name; }
}
