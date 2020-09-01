package org.alliancegenome.neo4j.entity;

import com.fasterxml.jackson.annotation.JsonView;
import lombok.Getter;
import lombok.Setter;
import org.alliancegenome.api.entity.PresentationEntity;
import org.alliancegenome.neo4j.entity.node.DOTerm;
import org.alliancegenome.neo4j.view.View;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.io.Serializable;
import java.util.Objects;

@Getter
@Setter
@Schema(name = "DiseaseModel", description = "POJO that represents a DiseaseModel")
public class DiseaseModel implements Comparable<DiseaseModel>, Serializable, PresentationEntity {

    @JsonView({View.PrimaryAnnotation.class, View.API.class})
    protected DOTerm disease;
    @JsonView({View.PrimaryAnnotation.class, View.API.class})
    private String associationType;
    private String modelName;

    public DiseaseModel(DOTerm disease, String associationType) {
        this.disease = disease;
        this.associationType = associationType;
    }

    public DiseaseModel() {
    }

    @JsonView({View.PrimaryAnnotation.class, View.API.class})
    public String getDiseaseModel() {
        String response = "";
        if (associationType.contains("NOT"))
            response += "does not model ";
        response += disease.getName();
        return response;
    }

    @JsonView({View.PrimaryAnnotation.class, View.API.class})
    public void setDiseaseModel(String name) {
        modelName = name;
    }

    @Override
    public int compareTo(DiseaseModel o) {
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DiseaseModel that = (DiseaseModel) o;
        return Objects.equals(disease, that.disease) &&
                Objects.equals(associationType, that.associationType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(disease, associationType);
    }
}
