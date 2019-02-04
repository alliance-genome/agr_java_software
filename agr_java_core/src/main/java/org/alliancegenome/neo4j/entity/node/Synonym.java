package org.alliancegenome.neo4j.entity.node;

import org.alliancegenome.neo4j.view.View;
import org.neo4j.ogm.annotation.NodeEntity;

import com.fasterxml.jackson.annotation.JsonView;

import lombok.Getter;
import lombok.Setter;

@NodeEntity
@Getter @Setter
public class Synonym extends Identifier {

    private String primaryKey;
    @JsonView({View.Default.class})
    private String name;
}
