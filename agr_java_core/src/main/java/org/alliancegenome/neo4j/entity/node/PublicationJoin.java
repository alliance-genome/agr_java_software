package org.alliancegenome.neo4j.entity.node;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.Getter;
import lombok.Setter;
import org.alliancegenome.neo4j.view.View;
import org.apache.commons.collections.CollectionUtils;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import java.util.List;
import java.util.stream.Collectors;

@NodeEntity
@Getter
@Setter
@Schema(name = "PublicationJoin", description = "POJO that represents the Publication Join")
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
    private List<AffectedGenomicModel> models;

    // annotation inferred on
    @Relationship(type = "PRIMARY_GENETIC_ENTITY")
    private List<Allele> alleles;

    @JsonView({View.API.class})
    private String dateAssigned;

    @Override
    public String toString() {
        if (ecoCode == null) {
            String ret = "";
            if (publication != null)
                ret += publication.getPubId();
            if (CollectionUtils.isNotEmpty(alleles)) {
                ret += ":" + alleles.stream().map(Allele::getSymbol).collect(Collectors.joining(","));
            }
            return ret;
        }
        String ecos = ecoCode.stream()
                .map(SimpleTerm::getName)
                .collect(Collectors.joining(","));
        return publication.getPubId() + ":" + ecos;
    }

}
