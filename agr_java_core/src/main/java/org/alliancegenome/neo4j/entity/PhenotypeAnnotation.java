package org.alliancegenome.neo4j.entity;

import java.io.Serializable;
import java.util.List;

import org.alliancegenome.es.index.site.doclet.SourceDoclet;
import org.alliancegenome.neo4j.entity.node.Allele;
import org.alliancegenome.neo4j.entity.node.Gene;
import org.alliancegenome.neo4j.entity.node.GeneticEntity;
import org.alliancegenome.neo4j.entity.node.Publication;
import org.alliancegenome.neo4j.view.View;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
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
    private List<Publication> publications;

    @JsonIgnore
    public String getDocumentId() {
        return primaryKey;
    }

    @Override
    public int compareTo(PhenotypeAnnotation doc) {
        return 0;
    }

}
