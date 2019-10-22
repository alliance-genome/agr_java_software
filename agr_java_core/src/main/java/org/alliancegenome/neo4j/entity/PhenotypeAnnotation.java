package org.alliancegenome.neo4j.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.Getter;
import lombok.Setter;
import org.alliancegenome.es.index.site.doclet.SourceDoclet;
import org.alliancegenome.neo4j.entity.node.AffectedGenomicModel;
import org.alliancegenome.neo4j.entity.node.Allele;
import org.alliancegenome.neo4j.entity.node.Gene;
import org.alliancegenome.neo4j.entity.node.Publication;
import org.alliancegenome.neo4j.view.View;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
public class PhenotypeAnnotation implements Comparable<PhenotypeAnnotation>, Serializable {

    private String primaryKey;
    private SourceDoclet source;
    @JsonView({View.PhenotypeAPI.class})
    private String phenotype;
    @JsonView({View.PhenotypeAPI.class})
    private Gene gene;
    @JsonView({View.PhenotypeAPI.class})
    private Allele allele;
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

