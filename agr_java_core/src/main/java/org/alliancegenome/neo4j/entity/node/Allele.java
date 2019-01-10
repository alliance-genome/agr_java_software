package org.alliancegenome.neo4j.entity.node;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
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

@NodeEntity
@Getter
@Setter
public class Allele extends Neo4jEntity implements Comparable<Allele> {

    @JsonView({View.Default.class})
    private String primaryKey;
    @JsonView({View.Default.class})
    private String symbol;

    @Convert(value=DateConverter.class)
    private Date dateProduced;
    private String release;
    private String localId;
    private String globalId;
    @JsonView({View.Default.class})
    @JsonProperty(value="url")
    private String modCrossRefCompleteUrl;

    @Relationship(type = "FROM_SPECIES")
    private Species species;

    @Relationship(type = "ALSO_KNOWN_AS")
    private Set<Synonym> synonyms = new HashSet<>();
    
    // Converts the list of synonym objects to a list of strings
    @JsonView(value={View.Default.class})
    @JsonProperty(value="synonyms")
    public List<String> getSynonymList() {
        List<String> list = new ArrayList<String>();
        for(Synonym s: synonyms) {
            list.add(s.getName());
        }
        return list;
    }

    @Relationship(type = "ALSO_KNOWN_AS")
    private Set<SecondaryId> secondaryIds = new HashSet<>();

    @Relationship(type = "IS_ALLELE_OF", direction = Relationship.OUTGOING)
    private Gene gene;
    
    @JsonView({View.Default.class})
    @Relationship(type = "IS_IMPLICATED_IN")
    private List<DOTerm> diseases = new ArrayList<>();

    @Relationship(type = "ASSOCIATION", direction = Relationship.UNDIRECTED)
    private List<DiseaseEntityJoin> diseaseEntityJoins = new ArrayList<>();

    @Relationship(type = "HAS_PHENOTYPE")
    private List<Phenotype> phenotypes = new ArrayList<>();

    @JsonView({View.Default.class})
    @Relationship(type = "CROSS_REFERENCE")
    private List<CrossReference> crossReferences = new ArrayList<>();

    @Override
    public int compareTo(Allele o) {
        return 0;
    }
}
