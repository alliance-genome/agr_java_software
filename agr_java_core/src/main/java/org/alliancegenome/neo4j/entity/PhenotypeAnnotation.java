package org.alliancegenome.neo4j.entity;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonView;
import org.alliancegenome.es.index.site.doclet.PublicationDoclet;
import org.alliancegenome.es.index.site.doclet.SourceDoclet;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;
import org.alliancegenome.es.index.site.document.FeatureDocument;
import org.alliancegenome.es.index.site.document.GeneDocument;
import org.alliancegenome.neo4j.entity.node.Feature;
import org.alliancegenome.neo4j.entity.node.Gene;
import org.alliancegenome.neo4j.entity.node.Publication;
import org.alliancegenome.neo4j.view.View;

@Getter @Setter
public class PhenotypeAnnotation implements Comparable<PhenotypeAnnotation> {

    @JsonView({View.PhenotypeView.class})
    private String primaryKey;
    private SourceDoclet source;
    @JsonView({View.PhenotypeView.class})
    private String phenotype;
    @JsonView({View.PhenotypeView.class})
    private Gene gene;
    @JsonView({View.PhenotypeView.class})
    private Feature feature;
    @JsonView({View.PhenotypeView.class})
    private List<Publication> publications;
    @JsonView({View.PhenotypeView.class})
    private String geneticEntity;

    @JsonIgnore
    public String getDocumentId() {
        return primaryKey;
    }

    @Override
    public int compareTo(PhenotypeAnnotation doc) {
        return 0;
    }

    public String getGeneticEntity() {
        return feature != null ? "allele" : "gene";
    }
}
