package org.alliancegenome.api.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.Getter;
import lombok.Setter;
import org.alliancegenome.neo4j.view.View;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class DiseaseEntitySlim {

    public static final String ALL = "ALL";
    @JsonView(View.DiseaseAnnotation.class)
    private Map<String, DiseaseEntitySubgroupSlim> slimMap = new LinkedHashMap<>();

    public void addDiseaseEntitySubgroupSlim(DiseaseEntitySubgroupSlim slim) {
        slimMap.put(ALL,slim);
    }

}
