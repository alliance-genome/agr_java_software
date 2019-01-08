package org.alliancegenome.neo4j.entity.node;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    private String modCrossRefCompleteUrl;

    @Relationship(type = "FROM_SPECIES")
    private Species species;

    @JsonView({View.Default.class})
    @Relationship(type = "ALSO_KNOWN_AS")
    private Set<Synonym> synonyms = new HashSet<>();

    @Relationship(type = "ALSO_KNOWN_AS")
    private Set<SecondaryId> secondaryIds = new HashSet<>();

    @Relationship(type = "IS_ALLELE_OF", direction = Relationship.OUTGOING)
    private Gene gene;

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
