package org.alliancegenome.api.entity;

import com.fasterxml.jackson.annotation.JsonView;
import lombok.Getter;
import lombok.Setter;
import org.alliancegenome.neo4j.view.View;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class DiseaseRibbonSection {

    @JsonView({View.DiseaseAnnotation.class})
    private String id;
    @JsonView({View.DiseaseAnnotation.class})
    private String label;
    @JsonView({View.DiseaseAnnotation.class})
    private String taxonId;

    @JsonView({View.DiseaseAnnotation.class})
    private List<DiseaseSectionSlim> slims = new ArrayList<>();

    public void addDiseaseSlim(DiseaseSectionSlim slim) {
        slims.add(slim);
    }
}
