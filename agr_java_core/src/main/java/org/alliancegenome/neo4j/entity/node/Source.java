package org.alliancegenome.neo4j.entity.node;

import java.io.Serializable;
import java.util.Objects;

import org.alliancegenome.neo4j.entity.SpeciesType;
import org.alliancegenome.neo4j.view.View;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.*;

import lombok.*;

@Setter
@Getter
@JsonInclude()
@Schema(name="Source", description="POJO that represents the Source")
public class Source implements Serializable {

    @JsonView(value = {View.Default.class, View.API.class})
    private String name;
    @JsonView(value = {View.Default.class, View.API.class})
    private String url;

    private SpeciesType speciesType;

    @Override
    public String toString() {
        return name + " : " + url;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Source source = (Source) o;
        return Objects.equals(name, source.name) &&
                Objects.equals(url, source.url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, url);
    }
}
