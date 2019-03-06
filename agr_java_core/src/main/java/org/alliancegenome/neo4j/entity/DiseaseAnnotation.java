package org.alliancegenome.neo4j.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.Getter;
import lombok.Setter;
import org.alliancegenome.core.service.SortingField;
import org.alliancegenome.neo4j.entity.node.*;
import org.alliancegenome.neo4j.view.View;

import java.io.Serializable;
import java.util.*;

@Getter
@Setter
@JsonPropertyOrder({"disease", "gene", "allele", "geneticEntityType", "associationType", "evidenceCodes", "source", "publications"})
public class DiseaseAnnotation implements Comparable<DiseaseAnnotation>, Serializable {

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
    private int sortOrder;

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

    public DiseaseAnnotation() {
    }
}
