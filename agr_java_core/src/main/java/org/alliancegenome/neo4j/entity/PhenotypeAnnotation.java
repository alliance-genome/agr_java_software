package org.alliancegenome.neo4j.entity;

import java.util.List;

import org.alliancegenome.es.index.site.doclet.SourceDoclet;
import org.alliancegenome.neo4j.entity.node.Allele;
import org.alliancegenome.neo4j.entity.node.Gene;
import org.alliancegenome.neo4j.entity.node.Publication;
import org.alliancegenome.neo4j.view.View;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class PhenotypeAnnotation implements Comparable<PhenotypeAnnotation> {

    @JsonView({View.Phenotype.class})
    private String primaryKey;
    private SourceDoclet source;
    @JsonView({View.Phenotype.class})
    private String phenotype;
    @JsonView({View.Phenotype.class})
    private Gene gene;
    @JsonView({View.Phenotype.class})
    private Allele allele;
    @JsonView({View.Phenotype.class})
    private List<Publication> publications;
    @JsonView({View.Phenotype.class})
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
        return allele != null ? "allele" : "gene";
    }
}
