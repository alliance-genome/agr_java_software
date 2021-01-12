package org.alliancegenome.neo4j.entity.node;

import java.util.Objects;

import org.alliancegenome.neo4j.view.View;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.neo4j.ogm.annotation.NodeEntity;

import com.fasterxml.jackson.annotation.JsonView;

import lombok.*;

@NodeEntity
@Getter
@Setter
@Schema(name="MMOTerm", description="POJO that represents the MMO Term")
public class MMOTerm extends Ontology {

    private String primaryKey;
    @JsonView(View.Expression.class)
    private String name;
    @JsonView(View.Expression.class)
    private String displaySynonym;

    @Override
    public String toString() {
        return displaySynonym + " [" + primaryKey + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MMOTerm mmoTerm = (MMOTerm) o;
        return Objects.equals(primaryKey, mmoTerm.primaryKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(primaryKey);
    }
}
