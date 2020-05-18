package org.alliancegenome.neo4j.entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.alliancegenome.api.entity.PresentationEntity;
import org.alliancegenome.es.util.DateConverter;
import org.alliancegenome.neo4j.entity.node.Allele;
import org.alliancegenome.neo4j.entity.node.CrossReference;
import org.alliancegenome.neo4j.entity.node.DOTerm;
import org.alliancegenome.neo4j.entity.node.GeneticEntity;
import org.alliancegenome.neo4j.entity.node.PublicationJoin;
import org.alliancegenome.neo4j.entity.node.SequenceTargetingReagent;
import org.alliancegenome.neo4j.entity.node.SimpleTerm;
import org.alliancegenome.neo4j.entity.node.Source;
import org.alliancegenome.neo4j.entity.node.Species;
import org.alliancegenome.neo4j.view.View;
import org.apache.commons.collections.CollectionUtils;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.neo4j.ogm.annotation.typeconversion.Convert;

import com.fasterxml.jackson.annotation.JsonView;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(name="PrimaryAnnotatedEntity", description="POJO that represents a Primary Annotated Entity")
public class PrimaryAnnotatedEntity implements Comparable<PrimaryAnnotatedEntity>, Serializable, PresentationEntity {

    @JsonView({View.PrimaryAnnotation.class, View.API.class})
    protected String id;
    protected String entityJoinPk;
    @JsonView({View.PrimaryAnnotation.class, View.API.class})
    protected String name;
    @JsonView({View.PrimaryAnnotation.class, View.API.class})
    protected String displayName;
    @JsonView({View.PrimaryAnnotation.class, View.API.class})
    protected String url;
    @JsonView({View.PrimaryAnnotation.class, View.API.class})
    protected GeneticEntity.CrossReferenceType type;
    @JsonView({View.PrimaryAnnotation.class, View.API.class})
    protected CrossReference crossReference;
    @JsonView({View.PrimaryAnnotation.class, View.API.class})
    private Source source;

    @JsonView({View.PrimaryAnnotation.class, View.API.class})
    protected List<DOTerm> diseases;
    @JsonView({View.PrimaryAnnotation.class, View.API.class})
    private List<String> phenotypes;
    @JsonView({View.PrimaryAnnotation.class, View.API.class})
    private List<PublicationJoin> publicationEvidenceCodes;
    @JsonView({View.PrimaryAnnotation.class, View.API.class})
    private List<Allele> alleles;
    @JsonView({View.PrimaryAnnotation.class, View.API.class})
    private List<SequenceTargetingReagent> sequenceTargetingReagents;

    @Convert(value = DateConverter.class)
    private Date dateProduced;

    private List<DiseaseAnnotation> annotations;

    @JsonView({View.PrimaryAnnotation.class, View.Default.class})
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
        diseases.sort(Comparator.comparing(doTerm -> doTerm.getName().toLowerCase()));
    }

    public void addPhenotype(String phenotype) {
        if (phenotypes == null)
            phenotypes = new ArrayList<>();
        phenotypes.add(phenotype);
        phenotypes = new ArrayList<>(new HashSet<>(phenotypes));
        phenotypes.sort(Comparator.naturalOrder());
    }

    public void addPublicationEvidenceCode(List<PublicationJoin> pubJoins) {
        if (CollectionUtils.isEmpty(pubJoins))
            return;
        if (publicationEvidenceCodes == null)
            publicationEvidenceCodes = new ArrayList<>();

        publicationEvidenceCodes.addAll(pubJoins);
        publicationEvidenceCodes = new ArrayList<>(new HashSet<>(publicationEvidenceCodes));
    }

    public void addPublicationEvidenceCode(PublicationJoin pubJoin) {
        if (publicationEvidenceCodes == null)
            publicationEvidenceCodes = new ArrayList<>();
        publicationEvidenceCodes.add(pubJoin);
        // sort and make distinct by pub and evidence codes only
        // this assumes only PublicationJoin records that belong to this PAE
        Map<String, List<PublicationJoin>> keyMap = publicationEvidenceCodes.stream()
                .collect(Collectors.groupingBy(join -> {
                    String key = join.getPublication().getPrimaryKey();
                    if (CollectionUtils.isNotEmpty(join.getEcoCode())) {
                        key += join.getEcoCode().stream().map(SimpleTerm::getPrimaryKey).collect(Collectors.joining());
                    }
                    return key;
                }));
        publicationEvidenceCodes = keyMap.values().stream()
                .map(publicationJoins -> publicationJoins.get(0))
                .sorted(Comparator.comparing(o -> o.getPublication().getPrimaryKey()))
                .collect(Collectors.toList());
    }

    public void addPhenotypes(List<String> phenotypeList) {
        if (phenotypeList == null)
            return;
        if (phenotypes == null)
            phenotypes = new ArrayList<>();
        phenotypes.addAll(phenotypeList);
        phenotypes = phenotypes.stream()
                .distinct()
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.toList());
    }

    public void addDiseases(List<DOTerm> diseaseList) {
        if (diseaseList == null)
            return;
        if (diseases == null)
            diseases = new ArrayList<>();
        diseases.addAll(diseaseList);
        diseases = diseases.stream()
                .distinct()
                .sorted(Comparator.comparing(SimpleTerm::getName))
                .collect(Collectors.toList());
    }

    public void setDataProvider(String dataProvider) {
        source = new Source();
        source.setName(dataProvider);
    }
}
