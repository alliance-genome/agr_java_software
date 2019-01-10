package org.alliancegenome.neo4j.entity.node;

import org.alliancegenome.neo4j.entity.Neo4jEntity;
import org.alliancegenome.neo4j.view.View;
import org.neo4j.ogm.annotation.NodeEntity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@NodeEntity
public class CrossReference extends Neo4jEntity {

    @JsonView({View.API.class, View.Phenotype.class, View.Interaction.class, View.Expression.class})
    @JsonProperty(value="url")
    private String crossRefCompleteUrl;
    
    @JsonView({View.API.class, View.Phenotype.class, View.Interaction.class, View.Expression.class})
    private String localId;
    
    @JsonView({View.Phenotype.class, View.Interaction.class, View.Expression.class})
    private String globalCrossRefId;
    
    @JsonView({View.Phenotype.class, View.Interaction.class, View.Expression.class})
    private String prefix;
    
    @JsonView({View.Phenotype.class, View.Interaction.class, View.Expression.class})
    private String name;
    
    @JsonView({View.Phenotype.class, View.Interaction.class, View.Expression.class})
    private String displayName;
    
    @JsonView({View.API.class, View.Phenotype.class, View.Interaction.class, View.Expression.class})
    private String primaryKey;
    
    @JsonView({View.API.class, View.Phenotype.class, View.Interaction.class, View.Expression.class})
    private String crossRefType;

    @JsonView({View.API.class})
    @JsonProperty(value="displayName")
    public String getDisplayNameAPI() {
        if(displayName != null && displayName.length() > 0) {
            return displayName;
        } else {
            return name;
        }
    }
    
    @Override
    public String toString() {
        return displayName;
    }
}
