package org.alliancegenome.api.entity;

import java.util.*;

import org.alliancegenome.neo4j.view.View;

import com.fasterxml.jackson.annotation.JsonView;

import lombok.*;

@Getter
@Setter
public class DiseaseEntitySlim {

    public static final String ALL = "ALL";
    @JsonView(View.DiseaseAnnotation.class)
    private Map<String, EntitySubgroupSlim> slimMap = new LinkedHashMap<>();

    public void addDiseaseEntitySubgroupSlim(EntitySubgroupSlim slim) {
        slimMap.put(ALL,slim);
    }

}
