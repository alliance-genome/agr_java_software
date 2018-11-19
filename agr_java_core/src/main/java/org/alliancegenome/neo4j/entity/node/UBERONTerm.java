package org.alliancegenome.neo4j.entity.node;

import com.fasterxml.jackson.annotation.JsonView;
import lombok.Getter;
import lombok.Setter;
import org.alliancegenome.neo4j.view.View;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@NodeEntity
@Getter @Setter
public class UBERONTerm extends Ontology {

    @JsonView(View.ExpressionView.class)
    private String name;
    private String description;
    private String href;
    private String type;
    @JsonView(View.ExpressionView.class)
    private String primaryKey;
    private String is_obsolete;
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
