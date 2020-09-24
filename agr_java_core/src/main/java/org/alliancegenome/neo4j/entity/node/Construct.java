package org.alliancegenome.neo4j.entity.node;

import java.util.*;

import org.alliancegenome.api.entity.PresentationEntity;
import org.alliancegenome.neo4j.view.View;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.neo4j.ogm.annotation.*;

import com.fasterxml.jackson.annotation.*;

import lombok.*;

@NodeEntity(label = "Construct")
@Getter
@Setter
@Schema(name = "Construct", description = "POJO that represents the Construct")
public class Construct extends GeneticEntity implements Comparable<Construct>, PresentationEntity {

    public Construct() {
        this.crossReferenceType = CrossReferenceType.CONSTRUCT;
    }

    @JsonView({View.Default.class, View.API.class})
    @JsonProperty(value = "id")
    private String primaryKey;

    @JsonView({View.Default.class, View.API.class})
    private String nameText;

    private String name;

    @JsonIgnore
    // bi-directional mapping. exclude since Construct is used from the Allele
    // otherwise an infinite loop when JSON-ified
    @Relationship(type = "CONTAINS", direction = Relationship.INCOMING)
    private List<Allele> alleles;

    @Relationship(type = "IS_REGULATED_BY", direction = Relationship.INCOMING)
    private List<Gene> regulatedByGenes = new ArrayList<>();

    @Relationship(type = "IS_REGULATED_BY", direction = Relationship.INCOMING)
    private List<NonBGIConstructComponent> nonBGIConstructComponentsRegulation;

    @Relationship(type = "EXPRESSES", direction = Relationship.INCOMING)
    private List<Gene> expressedGenes = new ArrayList<>();

    @Relationship(type = "EXPRESSES", direction = Relationship.INCOMING)
    private List<NonBGIConstructComponent> nonBGIConstructComponents;

    @JsonView({View.AlleleAPI.class, View.TransgenicAlleleAPI.class})
    @Relationship(type = "TARGETS", direction = Relationship.INCOMING)
    private List<Gene> targetGenes = new ArrayList<>();

    @Relationship(type = "TARGETS", direction = Relationship.INCOMING)
    private List<NonBGIConstructComponent> nonBGIConstructComponentsTarget;


    @Override
    public int compareTo(Construct o) {
        return 0;
    }

    @Override
    public String toString() {
        return primaryKey + " : " + nameText;
    }

    @JsonView({View.Default.class, View.API.class})
    public String getName() {
        return nameText;
    }

    @JsonView({View.AlleleAPI.class, View.TransgenicAlleleAPI.class})
    public List<GeneticEntity> getExpressedGenes() {
        List<GeneticEntity> entities = new ArrayList<>(expressedGenes);
        addNonBGIConstructComponents(entities, nonBGIConstructComponents);
        return entities;
    }

    @JsonView({View.AlleleAPI.class, View.TransgenicAlleleAPI.class})
    public List<GeneticEntity> getRegulatedByGenes() {
        List<GeneticEntity> entities = new ArrayList<>(regulatedByGenes);
        addNonBGIConstructComponents(entities, nonBGIConstructComponentsRegulation);
        return entities;
    }

    @JsonView({View.AlleleAPI.class, View.TransgenicAlleleAPI.class})
    public List<GeneticEntity> getTargetGenes() {
        List<GeneticEntity> entities = new ArrayList<>(targetGenes);
        addNonBGIConstructComponents(entities, nonBGIConstructComponentsTarget);
        return entities;
    }

    private void addNonBGIConstructComponents(List<GeneticEntity> entities, List<NonBGIConstructComponent> nonBGIConstructComponents) {
        List<GeneticEntity> nonBgis = new ArrayList<>();
        if (nonBGIConstructComponents != null) {
            nonBGIConstructComponents.forEach(nonBGIConstructComponent -> {
                GeneticEntity nonBGIConstructComponentGene = new GeneticEntity();
                nonBGIConstructComponentGene.setSymbol(nonBGIConstructComponent.getPrimaryKey());
                nonBGIConstructComponentGene.setCrossReferenceType(CrossReferenceType.NON_BGI_CONSTRUCT_COMPONENTS);
                nonBgis.add(nonBGIConstructComponentGene);
            });
        }
        // sorting:
        // 1. genes then nonBGIConstructComponents
        // 2. genes by phylogenetic species then by case-insensitive symbol
        // 3. nonBGIs by case-insensitive symbol
        nonBgis.sort(Comparator.comparing(geneticEntity -> geneticEntity.getSymbol().toLowerCase()));
        Comparator<GeneticEntity> comparingSpecies = Comparator.comparing(geneticEntity -> geneticEntity.getSpecies().getPhylogeneticOrder());
        entities.sort(comparingSpecies.thenComparing(o -> o.getSymbol().toLowerCase()));
        entities.addAll(nonBgis);
    }

}
