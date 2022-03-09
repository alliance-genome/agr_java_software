package org.alliancegenome.neo4j.entity.node;

import java.util.*;

import org.alliancegenome.es.util.DateConverter;
import org.alliancegenome.neo4j.entity.Neo4jEntity;
import org.alliancegenome.neo4j.view.View;
import org.apache.commons.collections.CollectionUtils;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.neo4j.ogm.annotation.Relationship;
import org.neo4j.ogm.annotation.typeconversion.Convert;

import com.fasterxml.jackson.annotation.*;

import lombok.*;

@Getter
@Setter
@Schema(name = "GeneticEntity", description = "POJO that represents a Genetic Entity")
public class GeneticEntity extends Neo4jEntity {

    protected CrossReferenceType crossReferenceType;

    @JsonView({View.Default.class, View.API.class, View.AlleleVariantSequenceConverterForES.class})
    @JsonProperty(value = "id")
    protected String primaryKey;
    @JsonView({View.Default.class, View.API.class, View.AlleleVariantSequenceConverterForES.class})
    protected String symbol;

    protected String symbolWithSpecies;
    @Convert(value = DateConverter.class)
    private Date dateProduced;

    @JsonView({View.Default.class, View.API.class, View.AlleleVariantSequenceConverterForES.class})
    private String modCrossRefCompleteUrl = "";

    // only used for JsonView
    /// set when deserialized
    protected Map<String, Object> crossReferencesMap = null;

    @JsonView({View.API.class, View.PhenotypeAPI.class, View.DiseaseAnnotation.class, View.Orthology.class, View.GeneAlleleVariantSequenceAPI.class, View.AlleleVariantSequenceConverterForES.class})
    @Relationship(type = "FROM_SPECIES")
    protected Species species;

    @Relationship(type = "ALSO_KNOWN_AS")
    private List<Synonym> synonyms;

    @Relationship(type = "ALSO_KNOWN_AS")
    private List<SecondaryId> secondaryIds;

    @Relationship(type = "CROSS_REFERENCE")
    protected List<CrossReference> crossReferences;

    protected List<String> secondaryIdsList;
    protected List<String> crossReferencesList;
    protected List<String> synonymsList;


    // Only for manual construction (Neo needs to use the no-args constructor)
    public GeneticEntity(String primaryKey, CrossReferenceType crossReferenceType) {
        this.primaryKey = primaryKey;
        this.crossReferenceType = crossReferenceType;
    }

    public GeneticEntity() {
    }



    // Converts the list of synonym objects to a list of strings
    @JsonView(value = {View.API.class, View.GeneAllelesAPI.class, View.GeneAlleleVariantSequenceAPI.class, View.AlleleVariantSequenceConverterForES.class})
    @JsonProperty(value = "synonyms")
    public List<String> getSynonymList() {
        if (synonyms != null && CollectionUtils.isEmpty(synonymsList)) {
            synonymsList = new ArrayList<String>();
            for (Synonym s : synonyms) {
                synonymsList.add(s.getName());
            }
        }
        return synonymsList;
    }

    @JsonProperty(value = "synonyms")
    public void setSynonymList(List<String> synonymsList) {
        if (synonymsList == null)
            return;
        this.synonymsList = synonymsList;
    }

    // Converts the list of secondary ids objects to a list of strings
    @JsonView(value = {View.API.class, View.AlleleVariantSequenceConverterForES.class})
    @JsonProperty(value = "secondaryIds")
    public List<String> getSecondaryIdsList() {
        if (secondaryIds != null && CollectionUtils.isEmpty(secondaryIdsList)) {
            secondaryIdsList = new ArrayList<>();
            for (SecondaryId s : secondaryIds) {
                secondaryIdsList.add(s.getName());
            }
        }
        return secondaryIdsList;
    }

    @JsonProperty(value = "secondaryIds")
    public void setSecondaryIdsList(List<String> secondaryIdsList) {
        if (secondaryIdsList == null)
            return;
        this.secondaryIdsList = secondaryIdsList;
    }

    @JsonView({View.API.class, View.AlleleVariantSequenceConverterForES.class})
    public Map<String, Object> getCrossReferenceMap() {
        if (crossReferencesMap != null)
            return crossReferencesMap;

        if (crossReferences != null) {
            crossReferencesMap = new HashMap<>();
            List<CrossReference> othersList = new ArrayList<>();
            for (CrossReference cr : crossReferences) {
                String typeName = crossReferenceType.getDisplayName();
                // hard-coding WB speciality submission
                // Todo: Needs better modeling: use label=transgene in neo, or subclass, or something else
                if(cr.getCrossRefType() != null){
                    if (cr.getCrossRefType().startsWith("transgene")) {
                        typeName = "transgene";
                    }
                    if (cr.getCrossRefType().startsWith(typeName + "/")) {
                        typeName = cr.getCrossRefType().replace(typeName + "/", "");
                        crossReferencesMap.put(typeName, cr);
                    } else if (cr.getCrossRefType().equals(typeName)) {
                        crossReferencesMap.put("primary", cr);
                    } else if (cr.getCrossRefType().equals("generic_cross_reference")) {
                        othersList.add(cr);
                        crossReferencesMap.put("other", othersList);
                    }
                }
            }
        }
        return crossReferencesMap;
    }

    public void setCrossReferenceMap(Map<String, Object> crossReferencesMap) {
        if (crossReferencesMap == null)
            return;
        this.crossReferencesMap = crossReferencesMap;
    }

    public List<String> getCrossReferencesList() {
        if (crossReferences != null && CollectionUtils.isEmpty(crossReferencesList)) {
            crossReferencesList = new ArrayList<>();
            for (CrossReference crossReference : crossReferences) {
                crossReferencesList.add(crossReference.getName());
            }
        }
        return crossReferencesList;
    }

    public void setCrossReferencesList(List<String> crossReferencesList) {
        this.crossReferencesList = crossReferencesList;
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

        GENE("gene"), ALLELE("allele"), GENOTYPE("genotype"), FISH("fish", "affected_genomic_model"), STRAIN("strain"),
        VARIANT("variant"), TRANSGENE("transgene"), CONSTRUCT("construct"), NON_BGI_CONSTRUCT_COMPONENTS("nonBGIConstructComponents");

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