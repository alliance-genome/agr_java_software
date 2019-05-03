package org.alliancegenome.api.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.Getter;
import lombok.Setter;
import org.alliancegenome.neo4j.view.View;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class DiseaseEntitySlim {

    @JsonView(View.DiseaseAnnotation.class)
    private String id;
    @JsonView(View.DiseaseAnnotation.class)
    @JsonProperty("subgroup")
    private List<DiseaseEntitySubgroupSlim> slims = new ArrayList<>();

    public void addDiseaseEntitySubgroupSlim(DiseaseEntitySubgroupSlim slim) {
        slims.add(slim);
    }

}
