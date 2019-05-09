package org.alliancegenome.neo4j.entity.node;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.Getter;
import lombok.Setter;
import org.alliancegenome.core.service.SourceService;
import org.alliancegenome.es.util.DateConverter;
import org.alliancegenome.neo4j.entity.SpeciesType;
import org.alliancegenome.neo4j.view.View;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;
import org.neo4j.ogm.annotation.typeconversion.Convert;

import java.util.*;
import java.util.stream.Collectors;

@NodeEntity
@Getter
@Setter
@JsonPropertyOrder({"id", "name", "definition"})
public class ECOTerm extends SimpleTerm {

    @JsonView({View.DiseaseAPI.class})
    private String definition;

    private String is_obsolete;

    @Relationship(type = "ASSOCIATION", direction = Relationship.INCOMING)
    private List<PublicationEvidenceCodeJoin> publicationEntityJoins;

    @Override
    public String toString() {
        return primaryKey + ":" + name;
    }

}
