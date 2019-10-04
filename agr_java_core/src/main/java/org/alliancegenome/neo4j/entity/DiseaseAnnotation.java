package org.alliancegenome.neo4j.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.Getter;
import lombok.Setter;
import org.alliancegenome.neo4j.entity.node.*;
import org.alliancegenome.neo4j.view.View;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

@Getter
@Setter
@JsonPropertyOrder({"disease", "gene", "allele", "geneticEntityType", "associationType", "ecoCode", "source", "publications"})
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
    private AffectedGenomicModel model;
    @JsonView({View.DiseaseAnnotationAll.class})
    private Gene orthologyGene;
    @JsonView({View.DiseaseAnnotation.class})
    @JsonProperty(value = "allele")
    private Allele feature;
    @JsonView({View.DiseaseAnnotation.class})
    private List<PrimaryAnnotatedEntity> primaryAnnotatedEntities;
    @JsonView({View.DiseaseAnnotation.class})
    private List<Reference> references;
    @JsonView({View.DiseaseAnnotation.class})
    private List<Publication> publications;
    @JsonView({View.DiseaseAnnotation.class})
    @JsonProperty(value = "evidenceCodes")
    private List<ECOTerm> ecoCodes;
    @JsonView({View.DiseaseAnnotation.class})
    private String associationType;
    @JsonView({View.DiseaseCacher.class})
    private int sortOrder;
    @JsonView({View.DiseaseAnnotation.class})
    private List<Gene> orthologyGenes;
    @JsonView({View.DiseaseAnnotation.class})
    private List<PublicationEvidenceCodeJoin> publicationEvidenceCodeJoins;

    public void addOrthologousGene(Gene gene) {
        if (orthologyGenes == null)
            orthologyGenes = new ArrayList<>();
        orthologyGenes.add(gene);
    }

    public void addPrimaryAnnotatedEntity(PrimaryAnnotatedEntity entity) {
        if (primaryAnnotatedEntities == null)
            primaryAnnotatedEntities = new ArrayList<>();
        if (!primaryAnnotatedEntities.contains(entity))
            primaryAnnotatedEntities.add(entity);
    }


    public void addAllPrimaryAnnotatedEntities(List<PrimaryAnnotatedEntity> annotatedEntities) {
        if (annotatedEntities == null)
            return;
        if (primaryAnnotatedEntities == null)
            primaryAnnotatedEntities = new ArrayList<>();
        primaryAnnotatedEntities.addAll(annotatedEntities);
        primaryAnnotatedEntities = primaryAnnotatedEntities.stream()
                .distinct()
                .sorted(Comparator.comparing(PrimaryAnnotatedEntity::getName))
                .collect(Collectors.toList());
    }

    @JsonView({View.DiseaseCacher.class})
    // lists the agr_do slim parents
    private Set<String> parentIDs;

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

    public void setGeneticEntityType(String name) {
        //can be ignored as is it calculated from the existence from the feature attribute.
    }

    public DiseaseAnnotation() {
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DiseaseAnnotation that = (DiseaseAnnotation) o;
        return sortOrder == that.sortOrder &&
                Objects.equals(primaryKey, that.primaryKey) &&
                Objects.equals(source, that.source) &&
                Objects.equals(disease, that.disease) &&
                Objects.equals(gene, that.gene) &&
                Objects.equals(feature, that.feature) &&
                Objects.equals(references, that.references) &&
                Objects.equals(publications, that.publications) &&
                Objects.equals(ecoCodes, that.ecoCodes) &&
                Objects.equals(associationType, that.associationType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(source, disease, gene, feature, publications, ecoCodes, associationType);
    }

    @Override
    public String toString() {
        return gene != null ? disease.getPrimaryKey() + " : " + gene.getPrimaryKey() : disease.getPrimaryKey();
    }

}
