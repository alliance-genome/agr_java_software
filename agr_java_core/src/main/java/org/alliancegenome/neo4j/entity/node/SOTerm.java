package org.alliancegenome.neo4j.entity.node;

import org.alliancegenome.neo4j.view.View;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.neo4j.ogm.annotation.NodeEntity;

import com.fasterxml.jackson.annotation.*;

import lombok.*;

@NodeEntity
@Getter
@Setter
@Schema(name="SOTerm", description="POJO that represents the SO Term")
public class SOTerm extends Ontology {

    public static final String INSERTION = "SO:0000667";
    public static final String DELETION = "SO:0000159";

    @JsonView({View.Default.class})
    @JsonProperty(value = "id")
    private String primaryKey;
    @JsonView({View.Default.class, View.AlleleVariantSequenceConverterForES.class})
    private String name;

    public boolean isInsertion() {
        return primaryKey.equals(INSERTION);
    }

    public boolean isDeletion() {
        return primaryKey.equals(DELETION);
    }
}
