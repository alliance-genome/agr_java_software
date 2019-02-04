package org.alliancegenome.neo4j.entity;

import com.fasterxml.jackson.annotation.JsonView;
import lombok.Getter;
import lombok.Setter;
import org.alliancegenome.neo4j.view.View;

import java.util.Arrays;

@Getter
@Setter
public class EntitySummary {

    @JsonView({View.Default.class})
    private long numberOfAnnotations;
    @JsonView({View.Default.class})
    private long numberOfEntities;


}
