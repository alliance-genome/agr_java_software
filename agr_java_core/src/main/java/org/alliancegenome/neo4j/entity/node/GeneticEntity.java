package org.alliancegenome.neo4j.entity.node;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alliancegenome.es.util.DateConverter;
import org.alliancegenome.neo4j.entity.Neo4jEntity;
import org.alliancegenome.neo4j.view.View;
import org.neo4j.ogm.annotation.Relationship;
import org.neo4j.ogm.annotation.typeconversion.Convert;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GeneticEntity extends Neo4jEntity {

    protected CrossReferenceType crossReferenceType;

    @JsonView({View.Default.class, View.API.class})
    @JsonProperty(value = "id")
    protected String primaryKey;
    @JsonView({View.Default.class, View.API.class})
    protected String symbol;

    protected String symbolWithSpecies;
    @Convert(value = DateConverter.class)
    private Date dateProduced;

    @JsonView({View.Default.class, View.API.class, View.PhenotypeAPI.class, View.DiseaseAnnotation.class})
    @Relationship(type = "FROM_SPECIES")
    protected Species species;

    @Relationship(type = "ALSO_KNOWN_AS")
    private Set<Synonym> synonyms = new HashSet<>();

    // Converts the list of synonym objects to a list of strings
    @JsonView(value = {View.API.class})
    @JsonProperty(value = "synonyms")
    public List<String> getSynonymList() {
        List<String> list = new ArrayList<>();
        for (Synonym s : synonyms) {
            list.add(s.getName());
        }
        return list;
    }

    @JsonProperty(value = "synonyms")
    public void setSynonymList(List<String> list) {
        if (list != null) {
            list.forEach(syn -> {
                Synonym synonym = new Synonym();
                synonym.setName(syn);
                synonyms.add(synonym);
            });
        }
    }

    @Relationship(type = "ALSO_KNOWN_AS")
    private Set<SecondaryId> secondaryIds = new HashSet<>();

    // Converts the list of secondary ids objects to a list of strings
    @JsonView(value = {View.API.class})
    @JsonProperty(value = "secondaryIds")
    public List<String> getSecondaryIdsList() {
        List<String> list = new ArrayList<>();
        for (SecondaryId s : secondaryIds) {
            list.add(s.getName());
        }
        return list;
    }

    @JsonProperty(value = "secondaryIds")
    public void setSecondaryIdsList(List<String> list) {
        if (list != null) {
            list.forEach(idName -> {
                SecondaryId secondaryId = new SecondaryId();
                secondaryId.setName(idName);
                secondaryIds.add(secondaryId);
            });
        }
    }

    @Relationship(type = "CROSS_REFERENCE")
    private List<CrossReference> crossReferences = new ArrayList<>();

    // Only for manual construction (Neo needs to use the no-args constructor)
    public GeneticEntity(String primaryKey, CrossReferenceType crossReferenceType) {
        this.primaryKey = primaryKey;
        this.crossReferenceType = crossReferenceType;
    }

    public GeneticEntity() {
    }

    // only used for JsonView
    /// set when deserialized
    private Map<String, Object> map = null;

    @JsonView({View.API.class})
    @JsonProperty(value = "crossReferences")
    public Map<String, Object> getCrossReferenceMap() {
        if (map != null)
            return map;
        map = new HashMap<>();
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

    @JsonProperty(value = "crossReferences")
    public void setCrossReferenceMap(Map<String, Object> map) {
        if (map == null)
            return;
        this.map = map;
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

    public static CrossReferenceType getType(String dbName) {
        return Arrays.stream(CrossReferenceType.values())
                .filter(type -> type.dbName.equals(dbName))
                .findFirst()
                .orElse(null);
    }

    @JsonView({View.API.class})
    @JsonProperty(value = "type")
    public String getType() {
        if (crossReferenceType == null)
            return "N/A";
        return crossReferenceType.displayName;
    }

    @JsonProperty(value = "type")
    public void setType(String type) {
        crossReferenceType = CrossReferenceType.getCrossReferenceType(type);
    }

    public enum CrossReferenceType {

        GENE("gene"), ALLELE("allele"), GENOTYPE("genotype"), FISH("fish", "affected_genomic_model"), STRAIN("strain");

        private String displayName;
        private String dbName;

        CrossReferenceType(String name) {
            this.displayName = name;
            this.dbName = name;
        }

        CrossReferenceType(String displayName, String dbName) {
            this.displayName = displayName;
            this.dbName = dbName;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDbName() {
            return dbName;
        }

        public static CrossReferenceType getCrossReferenceType(String name) {
            return Arrays.stream(values())
                    .filter(type -> type.getDbName().equals(name))
                    .findFirst().orElse(null);
        }
    }
}
