package org.alliancegenome.neo4j.entity;

import com.fasterxml.jackson.annotation.JsonView;
import lombok.Getter;
import lombok.Setter;
import org.alliancegenome.neo4j.view.View;

import java.util.Arrays;

@Getter
@Setter
public class DiseaseSummary extends EntitySummary {

    @JsonView({View.Default.class})
    private Type type;

    public enum Type {
        EXPERIMENT, ORTHOLOGY;

        public static Type getType(String type) {
            return Arrays.stream(values())
                    .filter(value -> type.toLowerCase().equals(value.name().toLowerCase()))
                    .findFirst().orElse(null);
        }
    }
}
