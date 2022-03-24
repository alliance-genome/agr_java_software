package org.alliancegenome.neo4j.entity.node;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.Getter;
import lombok.Setter;
import org.alliancegenome.api.entity.PresentationEntity;
import org.alliancegenome.neo4j.view.View;
import org.apache.commons.collections.CollectionUtils;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@NodeEntity(label = "Feature")
@Getter
@Setter
@Schema(name = "Allele", description = "POJO that represents the Allele")
@JsonPropertyOrder({"id", "symbol", "species", "synonyms", "secondaryIds", "crossReferences", "symbolText", "diseases", "phenotypes", "hasDisease", "hasPhenotype", "url", "category", "type"})
public class Allele extends GeneticEntity implements Comparable<Allele>, PresentationEntity {

    public Allele() {
        this.crossReferenceType = CrossReferenceType.ALLELE;
        populateCategory();
    }

    public Allele(String primaryKey, CrossReferenceType crossReferenceType) {
        super(primaryKey, crossReferenceType);
        populateCategory();
    }

    private String release;
    private String localId;
    private String globalId;
    @JsonView({View.Default.class, View.AlleleVariantSequenceConverterForES.class})
    private String symbolText;
    private String symbolTextWithSpecies;
    @JsonView({View.AlleleAPI.class})
    private String description;

    public final static String ALLELE_WITH_ONE_VARIANT = "allele with one associated variant";
    public final static String ALLELE_WITH_MULTIPLE_VARIANT = "allele with multiple associated variants";

    @JsonView({View.AlleleAPI.class, View.TransgenicAlleleAPI.class, View.GeneAlleleVariantSequenceAPI.class, View.AlleleVariantSequenceConverterForES.class})
    @Relationship(type = "IS_ALLELE_OF")
    private Gene gene;

    @JsonView({View.GeneAllelesAPI.class, View.GeneAlleleVariantSequenceAPI.class, View.AlleleVariantSequenceConverterForES.class})
    @Relationship(type = "IS_IMPLICATED_IN", direction = Relationship.INCOMING)
    private List<DOTerm> diseases;

    @JsonView({View.AlleleAPI.class, View.GeneAllelesAPI.class})
    @Relationship(type = "VARIATION", direction = Relationship.INCOMING)
    private List<Variant> variants;

    @Relationship(type = "ASSOCIATION", direction = Relationship.UNDIRECTED)
    private List<DiseaseEntityJoin> diseaseEntityJoins;

    @Relationship(type = "ASSOCIATION")
    private List<PhenotypeEntityJoin> phenotypeEntityJoins;

    @Relationship(type = "HAS_PHENOTYPE")
    private List<Phenotype> phenotypes;

    @JsonView({View.AlleleAPI.class, View.TransgenicAlleleAPI.class})
    @Relationship(type = "CONTAINS")
    private List<Construct> constructs;

    @Override
    public int compareTo(Allele o) {
        return 0;
    }

    @Override
    public String toString() {
        return primaryKey + ":" + symbolText;
    }

    public void setPhenotypes(List<Phenotype> phenotypes) {
        this.phenotypes = phenotypes;
        phenotype = CollectionUtils.isNotEmpty(phenotypes);
    }

    private boolean phenotype;
    private boolean disease;

    @JsonView({View.API.class, View.GeneAllelesAPI.class, View.GeneAlleleVariantSequenceAPI.class, View.AlleleVariantSequenceConverterForES.class})
    @JsonProperty(value = "hasPhenotype")
    public Boolean hasPhenotype() {
        return phenotype;
    }

    @JsonProperty(value = "hasPhenotype")
    public void setPhenotype(boolean hasPhenotype) {
        this.phenotype = hasPhenotype;
    }

    @JsonView({View.API.class, View.GeneAllelesAPI.class, View.GeneAlleleVariantSequenceAPI.class, View.AlleleVariantSequenceConverterForES.class})
    public Boolean hasDisease() {
        return disease;
    }

    @JsonProperty(value = "hasDisease")
    public void setDisease(boolean hasDisease) {
        // this is a calculated property but the setter needs to be here
        // for deserialization purposes.
        this.disease = hasDisease;
    }

    @JsonView({View.API.class, View.GeneAllelesAPI.class})
    public void setVariants(List<Variant> variants) {
        this.variants = variants;
        populateCategory();
    }

    public void addPhenotypeEntityJoins(List<PhenotypeEntityJoin> joins) {
        if (joins == null)
            return;
        if (phenotypeEntityJoins == null)
            phenotypeEntityJoins = new ArrayList<>();
        phenotypeEntityJoins.addAll(joins);
        phenotypeEntityJoins = phenotypeEntityJoins.stream().distinct().collect(Collectors.toList());
    }

    public void populateCategory() {
        if (crossReferenceType != CrossReferenceType.ALLELE) {
            category = crossReferenceType.getDisplayName();
            return;
        }
        if (CollectionUtils.isEmpty(variants)) {
            category = crossReferenceType.getDisplayName();
            return;
        }
        if (variants.size() == 1) {
            category = ALLELE_WITH_ONE_VARIANT;
            return;
        }
        category = ALLELE_WITH_MULTIPLE_VARIANT;
    }

    @JsonView({View.API.class, View.GeneAllelesAPI.class, View.AlleleVariantSequenceConverterForES.class})
    public List<Variant> getVariants() {
        return variants;
    }

    @JsonView({View.API.class, View.GeneAlleleVariantSequenceAPI.class, View.AlleleVariantSequenceConverterForES.class})
    private String category;

    public String getCategory() {
        return category;
    }

    // Do not use this setter. It's only used for deserialization purposes
    public void setCategory(String category) {
        this.category = category;
    }
}
