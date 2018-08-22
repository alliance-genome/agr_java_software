package org.alliancegenome.neo4j.entity.node;

import org.alliancegenome.neo4j.entity.Neo4jEntity;
import org.alliancegenome.neo4j.view.View;
import org.neo4j.ogm.annotation.NodeEntity;

import com.fasterxml.jackson.annotation.JsonView;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@NodeEntity
public class CrossReference extends Neo4jEntity {
    
    @JsonView({View.InteractionView.class})
    private String crossRefCompleteUrl;
    @JsonView({View.InteractionView.class})
    private String localId;
    @JsonView({View.InteractionView.class})
    private String globalCrossRefId;
    @JsonView({View.InteractionView.class})
    private String prefix;
    @JsonView({View.InteractionView.class})
    private String name;
    @JsonView({View.InteractionView.class})
    private String displayName;
    @JsonView({View.InteractionView.class})
    private String primaryKey;
    @JsonView({View.InteractionView.class})
    private String crossRefType;

}
