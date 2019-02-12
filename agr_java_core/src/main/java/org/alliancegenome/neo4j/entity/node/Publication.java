package org.alliancegenome.neo4j.entity.node;

import java.util.List;

import org.alliancegenome.neo4j.entity.Neo4jEntity;
import org.alliancegenome.neo4j.view.View;
import org.apache.commons.lang3.StringUtils;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;

import lombok.Getter;
import lombok.Setter;

@NodeEntity
@Getter
@Setter
public class Publication extends Neo4jEntity implements Comparable<Publication> {

    @JsonView({View.InteractionAPI.class, View.ExpressionAPI.class})
    private String primaryKey;
    @JsonView({View.InteractionAPI.class, View.ExpressionAPI.class})
    private String pubMedId;
    @JsonView({View.InteractionAPI.class, View.ExpressionAPI.class})
    private String pubMedUrl;
    @JsonView({View.InteractionAPI.class, View.ExpressionAPI.class})
    private String pubModId;
    @JsonView({View.InteractionAPI.class, View.ExpressionAPI.class})
    private String pubModUrl;
    @JsonView({View.ExpressionAPI.class})
    private String pubId;

    @Relationship(type = "ANNOTATED_TO")
    private List<EvidenceCode> evidence;

    public void setPubIdFromId() {
        if (StringUtils.isNotEmpty(pubMedId)) {
            pubId = pubMedId;
        } else {
            pubId = pubModId;
        }
    }

    @JsonView({View.PhenotypeAPI.class, View.ExpressionAPI.class, View.DiseaseAnnotation.class})
    @JsonProperty("id")
    private String getPublicationId() {
        if (StringUtils.isNotEmpty(pubMedId)) {
            return pubMedId;
        } else {
            return pubModId;
        }
    }

    @JsonView({View.PhenotypeAPI.class, View.ExpressionAPI.class, View.DiseaseAnnotation.class})
    @JsonProperty("url")
    private String getPublicationUrl() {
        if (StringUtils.isNotEmpty(pubMedId)) {
            return pubMedUrl;
        } else {
            return pubModUrl;
        }
    }

    @Override
    public String toString() {
        return getPublicationId() + " : " + getPublicationUrl();
    }

    @Override
    public int compareTo(Publication o) {
        return getPublicationId().compareTo(o.getPublicationId());
    }

    public String getPubId() {
        if (StringUtils.isNotEmpty(pubMedId))
            return pubMedId;
        return pubModId;

    }

}
