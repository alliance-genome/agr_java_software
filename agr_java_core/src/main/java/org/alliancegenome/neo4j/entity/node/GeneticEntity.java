package org.alliancegenome.neo4j.entity.node;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.Getter;
import lombok.Setter;
import org.alliancegenome.neo4j.entity.Neo4jEntity;
import org.alliancegenome.neo4j.view.View;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@NodeEntity
@Getter
public class GeneticEntity extends Neo4jEntity {

    // Only for manual construction (Neo needs to use the no-args constructor)
    public GeneticEntity(String primaryKey, Type type) {
        this.primaryKey = primaryKey;
        this.type = type;
    }

    public GeneticEntity() {
    }

    @JsonView({View.Default.class, View.Phenotype.class})
    @JsonProperty(value = "id")
    protected String primaryKey;
    @JsonView({View.Default.class, View.Phenotype.class})
    protected String symbol;

    @JsonView({View.Default.class, View.Phenotype.class})
    @Relationship(type = "FROM_SPECIES")
    private Species species;

    @Relationship(type = "CROSS_REFERENCE")
    private List<CrossReference> crossReferences = new ArrayList<>();

    @JsonView({View.Default.class, View.Phenotype.class})
    @JsonProperty(value = "crossReferences")
    public Map<String, Object> getCrossReferenceMap() {
        Map<String, Object> map = new HashMap<>();

        List<CrossReference> othersList = new ArrayList<>();
        map.put("other", othersList);
        for (CrossReference cr : crossReferences) {
            String typeName = type.displayName;
            if (cr.getCrossRefType().startsWith(typeName + "/")) {
                typeName = cr.getCrossRefType().replace(typeName + "/", "");
                map.put(typeName, cr);
            } else if (cr.getCrossRefType().equals(typeName)) {
                map.put("primary", cr);
            } else if (cr.getCrossRefType().equals("generic_cross_reference")) {
                othersList.add(cr);
            }
        }
        return map;
    }

    protected Type type;

    @JsonView({View.Default.class, View.Phenotype.class})
    @JsonProperty(value = "type")
    public String getType(){
        return type.displayName;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public void setSpecies(Species species) {
        this.species = species;
    }

    public void setCrossReferences(List<CrossReference> crossReferences) {
        this.crossReferences = crossReferences;
    }

    public enum Type {
        GENE("gene"), ALLELE("allele");

        private String displayName;

        Type(String name) {
            this.displayName = name;
        }
    }
}
