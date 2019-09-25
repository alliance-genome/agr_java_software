package org.alliancegenome.neo4j.entity.node;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.Getter;
import lombok.Setter;
import org.alliancegenome.neo4j.view.View;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import java.util.List;

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
    private List<PublicationEvidenceCodeJoin> publicationEntityJoins;

    @Override
    public String toString() {
        return primaryKey + ":" + name;
    }

    @Override
    public int compareTo(ECOTerm o) {
        return displaySynonym.compareTo(o.getDisplaySynonym());
    }
}
