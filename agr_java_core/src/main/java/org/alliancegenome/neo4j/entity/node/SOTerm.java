package org.alliancegenome.neo4j.entity.node;

import org.alliancegenome.neo4j.view.View;
import org.neo4j.ogm.annotation.NodeEntity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;

import lombok.Getter;
import lombok.Setter;

@NodeEntity
@Getter @Setter
public class SOTerm extends Ontology {

    @JsonView({View.Default.class})
    @JsonProperty(value="id")
    private String primaryKey;
    @JsonView({View.Default.class})
    private String name;
}
