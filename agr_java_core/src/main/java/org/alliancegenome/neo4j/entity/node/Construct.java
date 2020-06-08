package org.alliancegenome.neo4j.entity.node;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.Getter;
import lombok.Setter;
import org.alliancegenome.api.entity.PresentationEntity;
import org.alliancegenome.neo4j.view.View;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import java.util.ArrayList;
import java.util.List;

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

    @Relationship(type = "CONTAINS", direction = Relationship.INCOMING)
    private List<Allele> alleles;

    @JsonView({View.AlleleAPI.class})
    @Relationship(type = "IS_REGULATED_BY", direction = Relationship.INCOMING)
    private List<Gene> regulatedByGenes = new ArrayList<>();

    @JsonView({View.AlleleAPI.class})
    @Relationship(type = "EXPRESSES", direction = Relationship.INCOMING)
    private List<Gene> expressedGenes = new ArrayList<>();

    @Relationship(type = "EXPRESSES", direction = Relationship.INCOMING)
    private List<NonBGIConstructComponent> nonBGIConstructComponents;

    @JsonView({View.AlleleAPI.class})
    @Relationship(type = "TARGETS", direction = Relationship.INCOMING)
    private List<Gene> targetGenes = new ArrayList<>();


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

    @JsonView({View.AlleleAPI.class})
    public List<GeneticEntity> getExpressedGenes() {
        List<GeneticEntity> entities = new ArrayList<>(expressedGenes);
        if (nonBGIConstructComponents != null) {
            nonBGIConstructComponents.forEach(nonBGIConstructComponent -> {
                GeneticEntity nonBGIConstructComponentGene = new GeneticEntity();
                nonBGIConstructComponentGene.setPrimaryKey(nonBGIConstructComponent.getPrimaryKey());
                nonBGIConstructComponentGene.setSymbol(nonBGIConstructComponent.getPrimaryKey());
                nonBGIConstructComponentGene.setCrossReferenceType(CrossReferenceType.NON_BGI_CONSTRUCT_COMPONENTS);
                entities.add(nonBGIConstructComponentGene);
            });
        }
        return entities;
    }
}
