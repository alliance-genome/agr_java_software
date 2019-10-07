package org.alliancegenome.neo4j.entity.node;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.Getter;
import lombok.Setter;
import org.alliancegenome.neo4j.view.View;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@NodeEntity
@Getter
@Setter
public class PublicationEvidenceCodeJoin extends Association {

    @JsonView({View.DiseaseAnnotation.class})
    protected String primaryKey;
    protected String joinType;

    @JsonView({View.DiseaseAnnotation.class})
    @Relationship(type = "ASSOCIATION", direction = Relationship.INCOMING)
    private Publication publication;

    @JsonView({View.DiseaseAnnotation.class})
    @JsonProperty(value = "evidenceCodes")
    @Relationship(type = "ASSOCIATION")
    private List<ECOTerm> ecoCode;

    // annotation inferred on
    @Relationship(type = "PRIMARY_GENETIC_ENTITY")
    private AffectedGenomicModel model;

    @Override
    public String toString() {
        String ecos = ecoCode.stream()
                .map(SimpleTerm::getName)
                .collect(Collectors.joining(","));
        return publication.getPubId() + ":" + ecoCode;
    }
}
