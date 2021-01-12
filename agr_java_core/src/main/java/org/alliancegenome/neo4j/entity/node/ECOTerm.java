package org.alliancegenome.neo4j.entity.node;

import java.util.*;

import org.alliancegenome.neo4j.view.View;
import org.neo4j.ogm.annotation.*;

import com.fasterxml.jackson.annotation.*;

import lombok.*;

@NodeEntity
@Getter
@Setter
@JsonPropertyOrder({"id", "name", "definition"})
public class ECOTerm extends SimpleTerm implements Comparable<ECOTerm> {

    @JsonView({View.DiseaseAPI.class})
    private String definition;

    @JsonView({View.DiseaseAnnotation.class})
    private String displaySynonym;

    private String isObsolete;

    @Relationship(type = "ASSOCIATION", direction = Relationship.INCOMING)
    private List<PublicationJoin> publicationEntityJoins;

    @Override
    public String toString() {
        return primaryKey + ":" + name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ECOTerm ecoTerm = (ECOTerm) o;
        return Objects.equals(primaryKey, ecoTerm.primaryKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(definition);
    }

    @Override
    public int compareTo(ECOTerm o) {
        return displaySynonym.compareTo(o.getDisplaySynonym());
    }
}
