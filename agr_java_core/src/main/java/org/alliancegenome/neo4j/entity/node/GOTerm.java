package org.alliancegenome.neo4j.entity.node;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
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

    @Relationship(type = "IS_A")
    private Set<GOTerm> isAParents = new HashSet<>();

    @Relationship(type = "PART_OF")
    private Set<GOTerm> partOfParents = new HashSet<>();

    public Set<GOTerm> getParentTerms() {
        Set<GOTerm> parentTerms = new HashSet<>();

        isAParents.stream().forEach(parent -> { parentTerms.addAll(parent.getParentTerms());});
        partOfParents.stream().forEach(parent -> {parentTerms.addAll(parent.getParentTerms());});
                
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
