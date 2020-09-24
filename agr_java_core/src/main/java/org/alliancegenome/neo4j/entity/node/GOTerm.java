package org.alliancegenome.neo4j.entity.node;

import java.util.*;

import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.neo4j.ogm.annotation.*;

import lombok.*;

@NodeEntity
@Getter @Setter
@Schema(name="GoTerm", description="POJO that represents GO Terms")
public class GOTerm extends Ontology {

    private String nameKey;
    private String name;
    private String definition;
    private String href;
    private String type;
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
    private Set<GOTerm> isAParents = new HashSet<>();

    @Relationship(type = "PART_OF")
    private Set<GOTerm> partOfParents = new HashSet<>();

    //GoDocument push-throughs, these fields can be removed from the
    //GoTerm object when we refactor the indexing to have direct access to
    //repository methods
    private Set<String> geneNameKeys = new HashSet<>();
    private Set<String> speciesNames = new HashSet<>();

    public Set<GOTerm> getParentTerms() {
        Set<GOTerm> parentTerms = new HashSet<>();

        isAParents.forEach(parent -> { parentTerms.addAll(parent.getParentTerms());});
        partOfParents.forEach(parent -> {parentTerms.addAll(parent.getParentTerms());});

        parentTerms.add(this);

        return parentTerms;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GOTerm goTerm = (GOTerm) o;

        return primaryKey.equals(goTerm.primaryKey);
    }

    @Override
    public int hashCode() {
        return primaryKey.hashCode();
    }


}
