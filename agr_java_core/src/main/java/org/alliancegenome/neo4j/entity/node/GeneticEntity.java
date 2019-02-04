package org.alliancegenome.neo4j.entity.node;

import com.fasterxml.jackson.annotation.JsonInclude;
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
@Setter
public class GeneticEntity extends Neo4jEntity {

    protected CrossReferenceType crossReferenceType;

    @JsonView({View.Default.class, View.Phenotype.class})
    @JsonProperty(value = "id")
    protected String primaryKey;
    @JsonView({View.Default.class, View.Phenotype.class})
    protected String symbol;

    @JsonView({View.Default.class, View.Phenotype.class})
    @Relationship(type = "FROM_SPECIES")
    protected Species species;

    @Relationship(type = "CROSS_REFERENCE")
    private List<CrossReference> crossReferences = new ArrayList<>();

    // Only for manual construction (Neo needs to use the no-args constructor)
    public GeneticEntity(String primaryKey, CrossReferenceType crossReferenceType) {
        this.primaryKey = primaryKey;
        this.crossReferenceType = crossReferenceType;
    }

    public GeneticEntity() {
    }

    @JsonView({View.API.class, View.Phenotype.class})
    @JsonProperty(value = "crossReferences")
    public Map<String, Object> getCrossReferenceMap() {
        Map<String, Object> map = new HashMap<>();

        List<CrossReference> othersList = new ArrayList<>();
        for (CrossReference cr : crossReferences) {
            String typeName = crossReferenceType.displayName;
            if (cr.getCrossRefType().startsWith(typeName + "/")) {
                typeName = cr.getCrossRefType().replace(typeName + "/", "");
                map.put(typeName, cr);
            } else if (cr.getCrossRefType().equals(typeName)) {
                map.put("primary", cr);
            } else if (cr.getCrossRefType().equals("generic_cross_reference")) {
                othersList.add(cr);
                map.put("other", othersList);
            }
        }
        return map;
    }

    private String url;

    // ToDo: the primary URL should be an attribute on the entity node
    @JsonView({View.GeneAllelesAPI.class, View.AlleleAPI.class, View.Default.class})
    @JsonProperty(value = "url")
    @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
    public String getUrl() {
        if (url != null)
            return url;
        if (getCrossReferenceMap() == null)
            return null;
        CrossReference primary = (CrossReference) getCrossReferenceMap().get("primary");
        if (primary == null)
            return null;
        url = primary.getCrossRefCompleteUrl();
        return url;
    }


    @JsonView({View.Phenotype.class})
    @JsonProperty(value = "type")
    public String getType() {
        return crossReferenceType.displayName;
    }

    public enum CrossReferenceType {

        GENE("gene"), ALLELE("allele");

        private String displayName;

        CrossReferenceType(String name) {
            this.displayName = name;
        }
    }
}
