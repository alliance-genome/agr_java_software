package org.alliancegenome.neo4j.entity.node;

import org.alliancegenome.neo4j.view.View;
import org.neo4j.ogm.annotation.NodeEntity;

import com.fasterxml.jackson.annotation.JsonView;

import lombok.Getter;
import lombok.Setter;

@NodeEntity
@Getter @Setter
public class MITerm extends Ontology {
    @JsonView({View.InteractionAPI.class})
    private String primaryKey;
    @JsonView({View.InteractionAPI.class})
    private String label;
    @JsonView({View.InteractionAPI.class})
    private String definition;
    @JsonView({View.InteractionAPI.class})
    private String url;
}
