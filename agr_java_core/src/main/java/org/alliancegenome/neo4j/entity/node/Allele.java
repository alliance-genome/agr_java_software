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

@NodeEntity(label = "Feature")
@Getter
@Setter
@Schema(name = "Allele", description = "POJO that represents the Allele")
@JsonPropertyOrder({"id", "symbol", "species", "synonyms", "secondaryIds", "crossReferences", "symbolText", "diseases", "phenotypes", "hasDisease", "hasPhenotype", "url", "category", "type"})
public class Allele extends GeneticEntity implements Comparable<Allele>, PresentationEntity {

    public Allele() {
        this.crossReferenceType = CrossReferenceType.ALLELE;
    }

    private String release;
    private String localId;
    private String globalId;
    private String modCrossRefCompleteUrl;
    @JsonView({View.Default.class})
    private String symbolText;
    private String symbolTextWithSpecies;
    @JsonView({View.AlleleAPI.class})
    private String description;

    public final static String ALLELE_WITH_ONE_VARIANT = "allele with one associated variant";
    public final static String ALLELE_WITH_MULTIPLE_VARIANT = "allele with multiple associated variants";

    @JsonView({View.AlleleAPI.class, View.TransgenicAlleleAPI.class, View.GeneAlleleVariantSequenceAPI.class})
    @Relationship(type = "IS_ALLELE_OF")
    private Gene gene;

    @JsonView({View.GeneAllelesAPI.class, View.GeneAlleleVariantSequenceAPI.class})
    @Relationship(type = "IS_IMPLICATED_IN", direction = Relationship.INCOMING)
    private List<DOTerm> diseases = new ArrayList<>();

    @JsonView({View.AlleleAPI.class, View.GeneAllelesAPI.class})
    @Relationship(type = "VARIATION", direction = Relationship.INCOMING)
    private List<Variant> variants = new ArrayList<>();

    @Relationship(type = "ASSOCIATION", direction = Relationship.UNDIRECTED)
    private List<DiseaseEntityJoin> diseaseEntityJoins = new ArrayList<>();

    @JsonView({View.GeneAllelesAPI.class})
    @Relationship(type = "HAS_PHENOTYPE")
    private List<Phenotype> phenotypes = new ArrayList<>();

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

    @JsonView({View.API.class, View.GeneAllelesAPI.class, View.GeneAlleleVariantSequenceAPI.class})
    @JsonProperty(value = "hasPhenotype")
    public Boolean hasPhenotype() {
        return phenotype;
    }

    @JsonProperty(value = "hasPhenotype")
    public void setPhenotype(boolean hasPhenotype) {
        this.phenotype = hasPhenotype;
    }

    @JsonView({View.API.class, View.GeneAlleleVariantSequenceAPI.class})
    public Boolean hasDisease() {
        return CollectionUtils.isNotEmpty(diseaseEntityJoins) || CollectionUtils.isNotEmpty(diseases);
    }

    @JsonProperty(value = "hasDisease")
    public void setDisease(boolean hasDisease) {
        // this is a calculated property but the setter needs to be here
        // for deserialization purposes.
    }

    @JsonView({View.API.class, View.GeneAllelesAPI.class})
    public void setVariants(List<Variant> variants) {
        this.variants = variants;
        populateCategory();
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

    @JsonView({View.API.class, View.GeneAllelesAPI.class})
    public List<Variant> getVariants() {
        return variants;
    }

    @JsonView({View.API.class, View.GeneAlleleVariantSequenceAPI.class})
    private String category;

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}
