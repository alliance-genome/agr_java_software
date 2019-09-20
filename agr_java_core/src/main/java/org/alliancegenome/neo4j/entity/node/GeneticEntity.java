package org.alliancegenome.neo4j.entity.node;

import java.util.*;

import org.alliancegenome.es.util.DateConverter;
import org.alliancegenome.neo4j.entity.Neo4jEntity;
import org.alliancegenome.neo4j.view.View;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;

import lombok.Getter;
import lombok.Setter;
import org.neo4j.ogm.annotation.typeconversion.Convert;

@NodeEntity
@Getter
@Setter
public class GeneticEntity extends Neo4jEntity {

    protected CrossReferenceType crossReferenceType;

    @JsonView({View.Default.class, View.API.class})
    @JsonProperty(value = "id")
    protected String primaryKey;
    @JsonView({View.Default.class, View.API.class})
    protected String symbol;
    @Convert(value = DateConverter.class)
    private Date dateProduced;

    @JsonView({View.Default.class, View.PhenotypeAPI.class})
    @Relationship(type = "FROM_SPECIES")
    protected Species species;

    @Relationship(type = "ALSO_KNOWN_AS")
    private Set<Synonym> synonyms = new HashSet<>();

    // Converts the list of synonym objects to a list of strings
    @JsonView(value = {View.GeneAllelesAPI.class, View.AlleleAPI.class})
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
    @JsonView(value = {View.GeneAllelesAPI.class, View.AlleleAPI.class})
    @JsonProperty(value = "secondaryIds")
    public List<String> getSecondaryIdsList() {
        List<String> list = new ArrayList<>();
        for (SecondaryId s : secondaryIds) {
            list.add(s.getName());
        }
        return list;
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


    @JsonView({View.API.class})
    @JsonProperty(value = "type")
    public String getType() {
        return crossReferenceType.displayName;
    }

    @JsonProperty(value = "type")
    public void setType(String type) {
        crossReferenceType = CrossReferenceType.getCrossReferenceType(type);
    }

    public enum CrossReferenceType {

        GENE("gene"), ALLELE("allele"), GENOTYPE("genotype"), FISH("fish");

        private String displayName;

        CrossReferenceType(String name) {
            this.displayName = name;
        }

        public String getDisplayName() {
            return displayName;
        }

        public static CrossReferenceType getCrossReferenceType(String name) {
            return Arrays.stream(values())
                    .filter(type -> type.getDisplayName().equals(name))
                    .findFirst().orElse(null);
        }
    }
}
