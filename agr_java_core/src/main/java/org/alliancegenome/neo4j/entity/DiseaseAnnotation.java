package org.alliancegenome.neo4j.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.Getter;
import lombok.Setter;
import org.alliancegenome.es.index.site.doclet.SourceDoclet;
import org.alliancegenome.neo4j.entity.node.Allele;
import org.alliancegenome.neo4j.entity.node.DOTerm;
import org.alliancegenome.neo4j.entity.node.Gene;
import org.alliancegenome.neo4j.entity.node.Publication;
import org.alliancegenome.neo4j.view.View;

import java.util.List;

@Getter @Setter
public class DiseaseAnnotation implements Comparable<DiseaseAnnotation> {

    @JsonView({View.DiseaseAnnotation.class})
    private String primaryKey;
    private SourceDoclet source;
    @JsonView({View.DiseaseAnnotation.class})
    private DOTerm disease;
    @JsonView({View.DiseaseAnnotation.class})
    private Gene gene;
    @JsonView({View.DiseaseAnnotation.class})
    private Gene orthologyGene;
    @JsonView({View.DiseaseAnnotation.class})
    private Allele feature;
    @JsonView({View.DiseaseAnnotation.class})
    private List<Publication> publications;
    @JsonView({View.DiseaseAnnotation.class})
    private String geneticEntity;
    @JsonView({View.DiseaseAnnotation.class})
    private String associationType;

    @JsonIgnore
    public String getDocumentId() {
        return primaryKey;
    }

    @Override
    public int compareTo(DiseaseAnnotation doc) {
        return 0;
    }

    public String getGeneticEntity() {
        return feature != null ? "allele" : "gene";
    }
}
