package org.alliancegenome.neo4j.entity.node;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.alliancegenome.es.util.DateConverter;
import org.alliancegenome.neo4j.view.View;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;
import org.neo4j.ogm.annotation.typeconversion.Convert;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;

import lombok.Getter;
import lombok.Setter;

@NodeEntity(label = "Feature")
@Getter
@Setter
public class Allele extends GeneticEntity implements Comparable<Allele> {

    public Allele() {
        this.crossReferenceType = CrossReferenceType.ALLELE;
    }

    @Convert(value = DateConverter.class)
    private Date dateProduced;
    private String release;
    private String localId;
    private String globalId;
    private String modCrossRefCompleteUrl;
    @JsonView({View.Default.class})
    private String symbolText;

    @Relationship(type = "IS_ALLELE_OF")
    private Gene gene;

    @JsonView({View.GeneAllelesAPI.class, View.AlleleAPI.class})
    @Relationship(type = "IS_IMPLICATED_IN")
    private List<DOTerm> diseases = new ArrayList<>();

    @JsonView({View.GeneAllelesAPI.class, View.AlleleAPI.class})
    @Relationship(type = "VARIATION", direction = Relationship.INCOMING)
    private List<Variant> variants = new ArrayList<>();

    @Relationship(type = "ASSOCIATION", direction = Relationship.UNDIRECTED)
    private List<DiseaseEntityJoin> diseaseEntityJoins = new ArrayList<>();

    @JsonView({View.GeneAllelesAPI.class, View.AlleleAPI.class})
    @Relationship(type = "HAS_PHENOTYPE")
    private List<Phenotype> phenotypes = new ArrayList<>();

    @Override
    public int compareTo(Allele o) {
        return 0;
    }

}
