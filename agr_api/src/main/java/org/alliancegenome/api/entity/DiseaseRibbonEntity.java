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
    @JsonProperty("taxon_id")
    private String taxonID;
    @JsonProperty("taxon_label")
    private String taxonName;
    @JsonView(View.DiseaseAnnotation.class)
    @JsonProperty("nb_classes")
    private int numberOfClasses;
    @JsonView(View.DiseaseAnnotation.class)
    @JsonProperty("nb_annotations")
    private int numberOfAnnotations;

    @JsonView(View.DiseaseAnnotation.class)
    @JsonProperty("groups")
    // <disease ID, DiseaseEntitySubgroupSlim
    private Map<String, Map<String, DiseaseEntitySubgroupSlim>> slims = new LinkedHashMap<>();

    public void addDiseaseSlim(DiseaseEntitySubgroupSlim slim) {
        String id = slim.getId();
        if (id == null)
            id = "nullID";
        Map<String, DiseaseEntitySubgroupSlim> subgroupSlimMap = new LinkedHashMap<>();
        subgroupSlimMap.put("ALL", slim);
        slims.put(id, subgroupSlimMap);
    }
}
