package org.alliancegenome.neo4j.entity.node;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.Getter;
import lombok.Setter;
import org.alliancegenome.neo4j.entity.Neo4jEntity;
import org.alliancegenome.neo4j.view.View;
import org.neo4j.ogm.annotation.NodeEntity;

@Getter
@Setter
@NodeEntity
public class CrossReference extends Neo4jEntity {

    @JsonView({View.API.class, View.Interaction.class, View.Expression.class})
    @JsonProperty(value = "url")
    private String crossRefCompleteUrl;

    @JsonView({View.Interaction.class})
    private String localId;

    @JsonView({View.Interaction.class})
    private String globalCrossRefId;

    @JsonView({View.Interaction.class})
    private String prefix;

    @JsonView({View.Interaction.class})
    private String name;

    @JsonView({View.API.class, View.Interaction.class})
    private String displayName;

    @JsonView({View.Interaction.class})
    private String primaryKey;

    @JsonView({View.Interaction.class})
    private String crossRefType;

    private Boolean loadedDB;
    private Boolean curatedDB;

    @JsonView({View.API.class})
    @JsonProperty(value = "name")
    public String getDisplayNameAPI() {
        if (displayName != null && displayName.length() > 0) {
            return displayName;
        } else {
            return name;
        }
    }

    @JsonProperty(value = "name")
    public void setDisplayNameAPI(String value) {
        displayName = value;
        name = value;
    }

    @Override
    public String toString() {
        return localId + ":" + displayName;
    }
}
