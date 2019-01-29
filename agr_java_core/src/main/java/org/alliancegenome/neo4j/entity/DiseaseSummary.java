package org.alliancegenome.neo4j.entity;

import com.fasterxml.jackson.annotation.JsonView;
import lombok.Getter;
import lombok.Setter;
import org.alliancegenome.neo4j.view.View;

import java.util.Arrays;

@Getter
@Setter
public class DiseaseSummary {

    @JsonView({View.Default.class})
    private Type type;
    @JsonView({View.Default.class})
    private long numberOfAnnotations;
    @JsonView({View.Default.class})
    private long numberOfDiseases;


    public enum Type {
        EXPERIMENT, ORTHOLOGY;

        public static Type getType(String type) {
            return Arrays.stream(values())
                    .filter(value -> type.toLowerCase().equals(value.name().toLowerCase()))
                    .findFirst().orElse(null);
        }
    }
}
