package org.alliancegenome.neo4j.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.Getter;
import lombok.Setter;
import org.alliancegenome.neo4j.entity.node.*;
import org.alliancegenome.neo4j.view.View;

import java.util.List;

@Getter
@Setter
public class DiseaseAnnotation implements Comparable<DiseaseAnnotation> {

    @JsonView({View.DiseaseAnnotation.class})
    private String primaryKey;
    @JsonView({View.DiseaseAnnotation.class})
    private Source source;
    @JsonView({View.DiseaseAnnotation.class})
    private DOTerm disease;
    @JsonView({View.DiseaseAnnotation.class})
    private Gene gene;
    @JsonView({View.DiseaseAnnotation.class})
    private Gene orthologyGene;
    @JsonView({View.DiseaseAnnotation.class})
    @JsonProperty(value = "allele")
    private Allele feature;
    @JsonView({View.DiseaseAnnotation.class})
    private List<Reference> references;
    @JsonView({View.DiseaseAnnotation.class})
    private List<Publication> publications;
    @JsonView({View.DiseaseAnnotation.class})
    private List<EvidenceCode> evidenceCodes;
    @JsonView({View.DiseaseAnnotation.class})
    private String associationType;
    private List<DiseaseEntityJoin> diseaseEntityJoinSet;

    @JsonIgnore
    public String getDocumentId() {
        return primaryKey;
    }

    @Override
    public int compareTo(DiseaseAnnotation doc) {
        return 0;
    }

    @JsonView({View.Default.class})
    public String getGeneticEntityType() {
        return feature != null ? "allele" : "gene";
    }
}
