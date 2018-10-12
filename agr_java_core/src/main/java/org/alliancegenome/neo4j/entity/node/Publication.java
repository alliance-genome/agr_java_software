package org.alliancegenome.neo4j.entity.node;

import java.util.List;

import org.alliancegenome.neo4j.entity.Neo4jEntity;
import org.alliancegenome.neo4j.view.View;
import org.apache.commons.lang3.StringUtils;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import com.fasterxml.jackson.annotation.JsonView;

import lombok.Getter;
import lombok.Setter;

@NodeEntity
@Getter
@Setter
public class Publication extends Neo4jEntity {

    @JsonView({View.InteractionView.class, View.ExpressionView.class})
    private String primaryKey;
    @JsonView({View.InteractionView.class, View.ExpressionView.class})
    private String pubMedId;
    @JsonView({View.InteractionView.class, View.ExpressionView.class})
    private String pubMedUrl;
    @JsonView({View.InteractionView.class, View.ExpressionView.class})
    private String pubModId;
    @JsonView({View.InteractionView.class, View.ExpressionView.class})
    private String pubModUrl;
    @JsonView({View.ExpressionView.class})
    private String pubId;

    @Relationship(type = "ANNOTATED_TO")
    private List<EvidenceCode> evidence;

    public void setPubIdFromId() {
        if (StringUtils.isNotEmpty(pubMedId))
            pubId = pubMedId;
        else
            pubId = pubModId;
    }

    @Override
    public String toString() {
        return primaryKey;
    }
}
