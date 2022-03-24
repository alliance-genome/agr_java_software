package org.alliancegenome.neo4j.entity.node;

import org.alliancegenome.neo4j.entity.Neo4jEntity;
import org.alliancegenome.neo4j.view.View;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.neo4j.ogm.annotation.NodeEntity;

import com.fasterxml.jackson.annotation.*;

import lombok.*;

@Getter
@Setter
@NodeEntity
@Schema(name="CrossReference", description="POJO that represents CrossReferences")
public class CrossReference extends Neo4jEntity {

    @JsonView({View.API.class, View.Interaction.class, View.Expression.class,View.AlleleVariantSequenceConverterForES.class})
    private String crossRefCompleteUrl;

    @JsonView({View.Interaction.class})
    private String localId;

    @JsonView({View.Interaction.class})
    private String globalCrossRefId;

    @JsonView({View.Interaction.class})
    private String prefix;

    @JsonView({View.API.class, View.Interaction.class,View.AlleleVariantSequenceConverterForES.class})
    private String name;

    @JsonView({View.API.class, View.Interaction.class,View.AlleleVariantSequenceConverterForES.class})
    private String displayName;

    @JsonView({View.Interaction.class})
    private String primaryKey;

    @JsonView({View.Interaction.class})
    private String crossRefType;

    private Boolean loadedDB;
    private Boolean curatedDB;

    @Override
    public String toString() {
        return localId + ":" + displayName;
    }
}
