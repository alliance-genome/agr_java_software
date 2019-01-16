package org.alliancegenome.neo4j.entity.node;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import org.alliancegenome.es.util.DateConverter;
import org.alliancegenome.neo4j.entity.Neo4jEntity;
import org.alliancegenome.neo4j.view.View;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;
import org.neo4j.ogm.annotation.typeconversion.Convert;

import lombok.Getter;
import lombok.Setter;

@NodeEntity(label = "Feature")
@Getter
@Setter
public class Allele extends GeneticEntity implements Comparable<Allele> {

    @Convert(value = DateConverter.class)
    private Date dateProduced;
    private String release;
    private String localId;
    private String globalId;
    @JsonView({View.AlleleAPI.class})
    @JsonProperty(value = "url")
    private String modCrossRefCompleteUrl;

    @Relationship(type = "ALSO_KNOWN_AS")
    private Set<Synonym> synonyms = new HashSet<>();

    // Converts the list of synonym objects to a list of strings
    @JsonView(value = {View.AlleleAPI.class})
    @JsonProperty(value = "synonyms")
    public List<String> getSynonymList() {
        List<String> list = new ArrayList<>();
        for (Synonym s : synonyms) {
            list.add(s.getName());
        }
        return list;
    }

    @Relationship(type = "ALSO_KNOWN_AS")
    private Set<SecondaryId> secondaryIds = new HashSet<>();

    // Converts the list of secondary ids objects to a list of strings
    @JsonView(value = {View.GeneAPI.class})
    @JsonProperty(value = "secondaryIds")
    public List<String> getSecondaryIdsList() {
        List<String> list = new ArrayList<>();
        for (SecondaryId s : secondaryIds) {
            list.add(s.getName());
        }
        return list;
    }

    @Relationship(type = "IS_ALLELE_OF", direction = Relationship.OUTGOING)
    private Gene gene;

    @JsonView({View.AlleleAPI.class})
    @Relationship(type = "IS_IMPLICATED_IN")
    private List<DOTerm> diseases = new ArrayList<>();

    @Relationship(type = "ASSOCIATION", direction = Relationship.UNDIRECTED)
    private List<DiseaseEntityJoin> diseaseEntityJoins = new ArrayList<>();

    @Relationship(type = "HAS_PHENOTYPE")
    private List<Phenotype> phenotypes = new ArrayList<>();

    @Relationship(type = "CROSS_REFERENCE")
    private List<CrossReference> crossReferences = new ArrayList<>();

    @Override
    public int compareTo(Allele o) {
        return 0;
    }
}
