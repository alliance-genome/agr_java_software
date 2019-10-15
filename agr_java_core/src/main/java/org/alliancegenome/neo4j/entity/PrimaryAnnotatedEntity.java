package org.alliancegenome.neo4j.entity;

import com.fasterxml.jackson.annotation.JsonView;
import lombok.Getter;
import lombok.Setter;
import org.alliancegenome.es.util.DateConverter;
import org.alliancegenome.neo4j.entity.node.*;
import org.alliancegenome.neo4j.view.View;
import org.neo4j.ogm.annotation.typeconversion.Convert;

import java.io.Serializable;
import java.util.*;

@Getter
@Setter
public class PrimaryAnnotatedEntity implements Comparable<PrimaryAnnotatedEntity>, Serializable {

    @JsonView({View.Default.class, View.API.class})
    protected String id;
    @JsonView({View.Default.class, View.API.class})
    protected String name;
    @JsonView({View.Default.class, View.API.class})
    protected String displayName;
    @JsonView({View.Default.class, View.API.class})
    protected String url;
    @JsonView({View.Default.class, View.API.class})
    protected GeneticEntity.CrossReferenceType type;
    @JsonView({View.Default.class, View.API.class})
    protected CrossReference crossReference;
    @JsonView({View.API.class})
    private Source source;

    @JsonView({View.Default.class, View.API.class})
    protected List<DOTerm> diseases;
    @JsonView({View.Default.class, View.API.class})
    private List<String> phenotypes;
    @JsonView({View.API.class})
    private List<PublicationJoin> publicationEvidenceCodes;
    @JsonView({View.API.class})
    private List<Allele> alleles;
    @JsonView({View.API.class})
    private List<SequenceTargetingReagent> sequenceTargetingReagents;

    @Convert(value = DateConverter.class)
    private Date dateProduced;

    private List<DiseaseAnnotation> annotations;

    @JsonView({View.Default.class})
    protected Species species;

    @Override
    public int compareTo(PrimaryAnnotatedEntity o) {
        return 0;
    }


    @Override
    public String toString() {
        return id + ":" + name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PrimaryAnnotatedEntity that = (PrimaryAnnotatedEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public void addDisease(DOTerm disease) {
        if (diseases == null)
            diseases = new ArrayList<>();
        diseases.add(disease);
        diseases = new ArrayList<>(new HashSet<>(diseases));
    }

    public void addPhenotype(String phenotype) {
        if (phenotypes == null)
            phenotypes = new ArrayList<>();
        phenotypes.add(phenotype);
        phenotypes = new ArrayList<>(new HashSet<>(phenotypes));
    }

    public void addPublicationEvidenceCode(PublicationJoin pubJoin) {
        if (publicationEvidenceCodes == null)
            publicationEvidenceCodes = new ArrayList<>();
        publicationEvidenceCodes.add(pubJoin);
    }

    public void addPhenotypes(List<String> phenotypeList) {
        if (phenotypes == null)
            phenotypes = new ArrayList<>();
        phenotypes.addAll(phenotypeList);
    }

    public void setDataProvider(String dataProvider) {
        source = new Source();
        source.setName(dataProvider);
    }
}
