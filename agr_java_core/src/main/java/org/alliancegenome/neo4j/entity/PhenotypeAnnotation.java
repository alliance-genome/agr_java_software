package org.alliancegenome.neo4j.entity;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

import org.alliancegenome.neo4j.entity.node.*;
import org.alliancegenome.neo4j.view.View;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.*;

import lombok.*;

@Getter
@Setter
@Schema(name="PhenotypeAnnotation", description="POJO that represents a  Phenotype Annotation")
public class PhenotypeAnnotation implements Comparable<PhenotypeAnnotation>, Serializable {

    private String primaryKey;
    @JsonView({View.PhenotypeAPI.class})
    private Source source;
    @JsonView({View.PhenotypeAPI.class})
    private String phenotype;
    @JsonView({View.PhenotypeAPI.class})
    private Gene gene;
    @JsonView({View.PhenotypeAPI.class})
    private Allele allele;
    @JsonView({View.PhenotypeAPI.class})
    private AffectedGenomicModel model;
    @JsonView({View.PhenotypeAPI.class})
    private List<AffectedGenomicModel> models;
    @JsonView({View.PhenotypeAPI.class})
    private List<Publication> publications;
    @JsonView({View.PhenotypeAPI.class})
    private List<PrimaryAnnotatedEntity> primaryAnnotatedEntities;


    @JsonIgnore
    public String getDocumentId() {
        return primaryKey;
    }

    @Override
    public int compareTo(PhenotypeAnnotation doc) {
        return 0;
    }

    public void addPrimaryAnnotatedEntity(PrimaryAnnotatedEntity entity) {
        if (primaryAnnotatedEntities == null)
            primaryAnnotatedEntities = new ArrayList<>();
        if (!primaryAnnotatedEntities.contains(entity))
            primaryAnnotatedEntities.add(entity);
    }

    public void addPrimaryAnnotatedEntities(List<PrimaryAnnotatedEntity> entity) {
        if (primaryAnnotatedEntities == null)
            primaryAnnotatedEntities = new ArrayList<>();
        primaryAnnotatedEntities.addAll(entity);
        primaryAnnotatedEntities = primaryAnnotatedEntities.stream().distinct().collect(Collectors.toList());
    }

    public void addPublications(List<Publication> pubs) {
        if (publications == null)
            publications = new ArrayList<>();
        publications.addAll(pubs);
        publications = publications.stream().distinct().collect(Collectors.toList());
    }

    public void setPublications(List<Publication> pubs) {
        if (pubs == null)
            return;
        publications = pubs.stream()
                .distinct()
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.toList());
    }

    @Override
    public String toString() {
        String message = "";
        if (gene != null)
            message += gene.getPrimaryKey();
        if (primaryAnnotatedEntities != null)
            message += ":" + primaryAnnotatedEntities.stream().map(PrimaryAnnotatedEntity::getName).collect(Collectors.joining(", "));
        return message + ": " + phenotype;
    }
}

