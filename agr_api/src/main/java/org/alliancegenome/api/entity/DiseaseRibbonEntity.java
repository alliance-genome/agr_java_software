package org.alliancegenome.api.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.Getter;
import lombok.Setter;
import org.alliancegenome.neo4j.view.View;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Setter
public class DiseaseRibbonEntity {

    @JsonView(View.DiseaseAnnotation.class)
    private String id;
    @JsonView(View.DiseaseAnnotation.class)
    private String label;
    @JsonView(View.DiseaseAnnotation.class)
    private String taxonID;
    @JsonView(View.DiseaseAnnotation.class)
    private String annotationLabel;

    @JsonView(View.DiseaseAnnotation.class)
    @JsonProperty("groups")
    // <disease ID, DiseaseEntitySubgroupSlim
    private Map<String, DiseaseEntitySubgroupSlim> slims = new LinkedHashMap<>();

    public void addDiseaseSlim(DiseaseEntitySubgroupSlim slim) {
        String id = slim.getId();
        if (id == null)
            id = "nullID";
        slims.put(id, slim);
    }
}
