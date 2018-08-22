package org.alliancegenome.neo4j.entity.node;

import java.util.List;

import org.alliancegenome.neo4j.entity.Neo4jEntity;
import org.alliancegenome.neo4j.view.View;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import com.fasterxml.jackson.annotation.JsonView;

import lombok.Getter;
import lombok.Setter;

@NodeEntity
@Getter
@Setter
public class Publication extends Neo4jEntity {

    @JsonView(View.InteractionView.class)
    private String primaryKey;
    private String pubMedId;
    @JsonView(View.InteractionView.class)
    private String pubMedUrl;
    private String pubModId;
    @JsonView(View.InteractionView.class)
    private String pubModUrl;

    @Relationship(type = "ANNOTATED_TO")
    private List<EvidenceCode> evidence;

}
