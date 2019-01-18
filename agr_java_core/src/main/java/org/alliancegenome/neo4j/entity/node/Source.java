package org.alliancegenome.neo4j.entity.node;

import com.fasterxml.jackson.annotation.JsonView;
import lombok.Getter;
import lombok.Setter;
import org.alliancegenome.neo4j.view.View;

@Setter
@Getter
public class Source {

    @JsonView(value = {View.Default.class, View.API.class})
    private String name;
    @JsonView(value = {View.Default.class, View.API.class})
    private String url;
}
