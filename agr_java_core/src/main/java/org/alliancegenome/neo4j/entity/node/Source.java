package org.alliancegenome.neo4j.entity.node;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.Getter;
import lombok.Setter;
import org.alliancegenome.neo4j.entity.SpeciesType;
import org.alliancegenome.neo4j.view.View;

@Setter
@Getter
@JsonInclude()
public class Source {

    @JsonView(value = {View.Default.class, View.API.class})
    private String name;
    @JsonView(value = {View.Default.class, View.API.class})
    private String url;

    private SpeciesType speciesType;

    @Override
    public String toString() {
        return name + " : " + url;
    }
}
