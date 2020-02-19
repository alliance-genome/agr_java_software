package org.alliancegenome.neo4j.entity.node;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.Getter;
import lombok.Setter;
import org.alliancegenome.neo4j.view.View;
import org.neo4j.ogm.annotation.NodeEntity;

@NodeEntity
@Getter
@Setter
public class SOTerm extends Ontology {

    public static final String INSERTION = "SO:0000667";
    public static final String DELETION = "SO:0000159";

    @JsonView({View.Default.class})
    @JsonProperty(value = "id")
    private String primaryKey;
    @JsonView({View.Default.class})
    private String name;

    public boolean isInsertion() {
        return primaryKey.equals(INSERTION);
    }

    public boolean isDeletion() {
        return primaryKey.equals(DELETION);
    }
}
