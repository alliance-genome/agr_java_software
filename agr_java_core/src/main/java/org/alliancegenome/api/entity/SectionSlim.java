package org.alliancegenome.api.entity;

import java.io.Serializable;

import org.alliancegenome.neo4j.view.View;

import com.fasterxml.jackson.annotation.JsonView;

import lombok.*;

@Getter
@Setter
public class SectionSlim implements Serializable {

    @JsonView({View.DiseaseAnnotation.class, View.Expression.class})
    private String id;
    @JsonView({View.DiseaseAnnotation.class, View.Expression.class})
    private String label;
    @JsonView({View.DiseaseAnnotation.class, View.Expression.class})
    private String description;
    @JsonView({View.DiseaseAnnotation.class, View.Expression.class})
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

    @Override
    public String toString() {
        return id + ':' + label;
    }
}
