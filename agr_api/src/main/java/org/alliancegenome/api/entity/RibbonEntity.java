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
public class RibbonEntity {

    @JsonView({View.DiseaseAnnotation.class, View.Expression.class})
    private String id;
    @JsonView({View.DiseaseAnnotation.class, View.Expression.class})
    private String label;
    @JsonView({View.DiseaseAnnotation.class, View.Expression.class})
    @JsonProperty("taxon_id")
    private String taxonID;
    @JsonView({View.DiseaseAnnotation.class, View.Expression.class})
    @JsonProperty("taxon_label")
    private String taxonName;
    @JsonView({View.DiseaseAnnotation.class, View.Expression.class})
    @JsonProperty("nb_classes")
    private int numberOfClasses;
    @JsonView({View.DiseaseAnnotation.class, View.Expression.class})
    @JsonProperty("nb_annotations")
    private int numberOfAnnotations;

    @JsonView({View.DiseaseAnnotation.class, View.Expression.class})
    @JsonProperty("groups")
    // <disease ID, EntitySubgroupSlim
    private Map<String, Map<String, Object>> slims = new LinkedHashMap<>();

    public void addEntitySlim(EntitySubgroupSlim slim) {
        String id = slim.getId();
        if (id == null)
            id = "nullID";
        Map<String, Object> subgroupSlimMap = new LinkedHashMap<>();
        subgroupSlimMap.put("ALL", slim);
        if (slim.getAvailable() != null)
            subgroupSlimMap.put("available", slim.getAvailable().toString());
        slims.put(id, subgroupSlimMap);
    }
}
