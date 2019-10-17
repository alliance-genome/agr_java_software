package org.alliancegenome.neo4j.entity.node;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

import org.alliancegenome.neo4j.entity.Neo4jEntity;
import org.alliancegenome.neo4j.view.View;
import org.apache.commons.lang3.StringUtils;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.JsonView;

import lombok.Getter;
import lombok.Setter;

@NodeEntity
@Getter
@Setter
public class Publication extends Neo4jEntity implements Comparable<Publication>, Serializable {

    @JsonView({View.Interaction.class})
    private String primaryKey;
    @JsonView({View.Interaction.class})
    private String pubMedId;
    @JsonView({View.Interaction.class})
    private String pubMedUrl;
    @JsonView({View.Interaction.class})
    private String pubModId;
    @JsonView({View.Interaction.class})
    private String pubModUrl;
    private String pubId;
    private String pubUrl;

    @Relationship(type = "ANNOTATED_TO")
    private List<ECOTerm> evidence;

    public void setPubIdFromId() {
        if (StringUtils.isNotEmpty(pubMedId)) {
            pubId = pubMedId;
        } else {
            pubId = pubModId;
        }
    }

    @JsonView({View.API.class, View.DiseaseAnnotation.class})
    @JsonGetter("url")
    private String getPubUrl() {
        if (StringUtils.isNotEmpty(pubMedId)) {
            return pubMedUrl;
        } else {
            return pubModUrl;
        }
    }

    @JsonSetter("url")
    private void setPubUrl(String value) {
        pubMedUrl = value;
    }

    @Override
    public String toString() {
        return getPubId() + " : " + getPubUrl();
    }

    @Override
    public int compareTo(Publication o) {
        return getPubId().compareTo(o.getPubId());
    }

    @JsonView({View.API.class})
    @JsonGetter("id")
    public String getPubId() {
        if (StringUtils.isNotEmpty(pubMedId))
            return pubMedId;
        return pubModId != null ? pubModId : "";

    }

    @JsonSetter("id")
    private void setPubId(String value) {
        pubMedId = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Publication that = (Publication) o;
        return Objects.equals(getPubId(), that.getPubId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getPubId());
    }
}
