package org.alliancegenome.neo4j.entity.node;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.alliancegenome.neo4j.view.View;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@NodeEntity
@Getter
@Setter
public class PublicationJoin extends Association {

    @JsonView({View.API.class})
    protected String primaryKey;
    protected String joinType;

    @JsonView({View.API.class})
    @Relationship(type = "ASSOCIATION", direction = Relationship.INCOMING)
    private Publication publication;

    @JsonView({View.API.class})
    @JsonProperty(value = "evidenceCodes")
    @Relationship(type = "ASSOCIATION")
    private List<ECOTerm> ecoCode;

    // annotation inferred on
    @Relationship(type = "PRIMARY_GENETIC_ENTITY")
    private AffectedGenomicModel model;

    // annotation inferred on
    @Relationship(type = "PRIMARY_GENETIC_ENTITY")
    private List<Allele> alleles;

    @Override
    public String toString() {
        if (ecoCode == null)
            return publication.getPubId();
        String ecos = ecoCode.stream()
                .map(SimpleTerm::getName)
                .collect(Collectors.joining(","));
        return publication.getPubId() + ":" + ecos;
    }

}
