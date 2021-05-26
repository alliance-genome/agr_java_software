package org.alliancegenome.neo4j.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.Getter;
import lombok.Setter;
import org.alliancegenome.api.entity.PresentationEntity;
import org.alliancegenome.neo4j.entity.node.*;
import org.alliancegenome.neo4j.view.View;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

@Getter
@Setter
@JsonPropertyOrder({"disease", "gene", "allele", "geneticEntityType", "associationType", "ecoCode", "source", "publications"})
@Schema(name = "DiseaseAnnotation", description = "POJO that represents a Disease Annotation")
public class DiseaseAnnotation implements Comparable<DiseaseAnnotation>, Serializable, PresentationEntity {

    public static final String NOT_ASSOCIATION_TYPE = "not";

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
    // This attribute will go away and be replaced by publicationJoin objects that keep the pub/evCodes pairs
    private List<Publication> publications;
    // This attribute will go away and be replaced by publicationJoin objects that keep the pub/evCodes pairs
    private List<ECOTerm> ecoCodes;
    @JsonView({View.DiseaseAnnotation.class})
    private String associationType;
    @JsonView({View.DiseaseCacher.class})
    private int sortOrder;
    @JsonView({View.DiseaseAnnotation.class})
    private List<Gene> orthologyGenes;
    @JsonView({View.DiseaseAnnotation.class})
    private List<PublicationJoin> publicationJoins;
    @JsonView({View.DiseaseAnnotation.class})
    private List<Map<String, CrossReference>> providers;

    private Map<String, ExperimentalCondition> conditions;

    private Map<String, ExperimentalCondition> conditionModifiers;

    public void addOrthologousGene(Gene gene) {
        if (orthologyGenes == null)
            orthologyGenes = new ArrayList<>();
        orthologyGenes.add(gene);
    }

    public void addOrthologousGenes(List<Gene> genes) {
        if (genes == null)
            return;
        if (orthologyGenes == null)
            orthologyGenes = new ArrayList<>();
        orthologyGenes.addAll(genes);
        orthologyGenes = orthologyGenes.stream()
                .distinct()
                .sorted(Comparator.comparing(Gene::getSymbol))
                .collect(Collectors.toList());
    }

    public void addPrimaryAnnotatedEntity(PrimaryAnnotatedEntity entity) {
        if (primaryAnnotatedEntities == null)
            primaryAnnotatedEntities = new ArrayList<>();
        if (!primaryAnnotatedEntities.contains(entity))
            primaryAnnotatedEntities.add(entity);
    }

    public void addPrimaryAnnotatedEntityDuplicate(PrimaryAnnotatedEntity entity) {
        if (primaryAnnotatedEntities == null)
            primaryAnnotatedEntities = new ArrayList<>();
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
        String primaryKey = disease.getPrimaryKey() + " : ";
        if (gene != null)
            primaryKey += gene.getPrimaryKey();
        return primaryKey + " : " + associationType;
    }

    transient boolean remove = false;

    public void setPublicationJoins(List<PublicationJoin> publicationJoins) {
        addPublicationJoins(publicationJoins);
    }

    public void addPublicationJoins(List<PublicationJoin> joins) {
        if (joins == null)
            return;
        if (publicationJoins == null)
            publicationJoins = new ArrayList<>();
        publicationJoins.addAll(joins);
        publicationJoins = publicationJoins.stream()
                .distinct()
                .collect(Collectors.toList());

        if (publications == null)
            publications = new ArrayList<>();
        publications.addAll(publicationJoins.stream()
                .map(PublicationJoin::getPublication)
                .distinct()
                .collect(Collectors.toList()));
        publications = publications.stream()
                .distinct()
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.toList());
    }

    public Species getSpecies() {
        if (gene != null)
            return gene.getSpecies();
        if (feature != null)
            return feature.getSpecies();
        return model.getSpecies();
    }

    @JsonView({View.DiseaseAnnotation.class})
    @JsonProperty(value = "evidenceCodes")
    public List<ECOTerm> getEcoCodes() {
        if (publicationJoins == null)
            return null;
        return publicationJoins.stream()
                .map(PublicationJoin::getEcoCode)
                .flatMap(Collection::stream)
                .distinct()
                .collect(Collectors.toList());
    }

    @JsonProperty(value = "evidenceCodes")
    public void setEcoCodes(List<ECOTerm> ecoCodes) {
        this.ecoCodes = ecoCodes;
    }

    @JsonView({View.DiseaseAnnotation.class})
    public Map<String, ExperimentalCondition> getConditionModifiers() {
        return conditionModifiers;
    }

    public void addModifier(ConditionType conditionType, List<ExperimentalCondition> conditionModifier) {
        if (conditionModifier == null || conditionType == null)
            return;
        if (!conditionType.isModifier())
            throw new RuntimeException("No Modifier condition provided:" + conditionType);
        if (this.conditionModifiers == null)
            this.conditionModifiers = new HashMap<>();
        conditionModifier.forEach(condition -> conditionModifiers.put(conditionType.name(), condition));
    }

    public void setConditions(Map<String, ExperimentalCondition> conditions) {
        this.conditions = conditions;
    }

    public void setConditionModifiers(Map<String, ExperimentalCondition> conditionModifiers) {
        this.conditionModifiers = conditionModifiers;
    }

    @JsonView({View.DiseaseAnnotation.class})
    public Map<String, ExperimentalCondition> getConditions() {
        return conditions;
    }

    public void addConditions(ConditionType conditionType, List<ExperimentalCondition> conditions) {
        if (conditions == null || conditionType == null)
            return;
        if (!conditionType.isCondition())
            throw new RuntimeException("No condition type provided:" + conditionType);
        if (this.conditions == null)
            this.conditions = new HashMap<>();
        conditions.forEach(condition -> this.conditions.put(conditionType.name(), condition));
    }

    public enum ConditionType {
        HAS_CONDITION, INDUCES, AMELIORATES, EXACERBATES;

        public static boolean valueOfIgnoreCase(String conditionType) {
            return Arrays.stream(values()).anyMatch(conditionType1 -> conditionType1.name().equalsIgnoreCase(conditionType));
        }

        public boolean isModifier() {
            return this.equals(AMELIORATES) || this.equals(EXACERBATES);
        }

        public boolean isCondition() {
            return this.equals(HAS_CONDITION) || this.equals(INDUCES);
        }
    }


}
