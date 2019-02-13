package org.alliancegenome.neo4j.entity.node;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.Getter;
import lombok.Setter;
import org.alliancegenome.neo4j.entity.Neo4jEntity;
import org.alliancegenome.neo4j.view.View;
import org.apache.commons.lang3.StringUtils;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import java.util.List;

@NodeEntity
@Getter
@Setter
public class Publication extends Neo4jEntity implements Comparable<Publication> {

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
    private List<EvidenceCode> evidence;

    public void setPubIdFromId() {
        if (StringUtils.isNotEmpty(pubMedId)) {
            pubId = pubMedId;
        } else {
            pubId = pubModId;
        }
    }

    @JsonView({View.Phenotype.class, View.API.class, View.DiseaseAnnotation.class})
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
        pubUrl = value;
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
        pubId = value;
    }


}
