package org.alliancegenome.api.entity;

import com.fasterxml.jackson.annotation.JsonView;
import lombok.Getter;
import lombok.Setter;
import org.alliancegenome.neo4j.view.View;

import java.io.Serializable;

@Getter
@Setter
public class DiseaseSectionSlim implements Serializable {

    @JsonView({View.DiseaseAnnotation.class})
    private String id;
    @JsonView({View.DiseaseAnnotation.class})
    private String label;
    @JsonView({View.DiseaseAnnotation.class})
    private String description;
    @JsonView({View.DiseaseAnnotation.class})
    private String type = "Term";

    public void setTypeAll() {
        type = Type.ALL.getDisplayName();
    }

    public void setTypeOther() {
        type = Type.OTHER.getDisplayName();
    }


    public enum Type {
        TERM("Term"), ALL("All"), OTHER("Other");

        private String displayName;

        Type(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
