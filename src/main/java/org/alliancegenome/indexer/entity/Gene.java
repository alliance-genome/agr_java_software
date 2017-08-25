package org.alliancegenome.indexer.entity;

import lombok.Getter;
import lombok.Setter;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@NodeEntity
@Getter
@Setter
public class Gene extends Neo4jNode implements Comparable<Gene> {

    private String primaryKey;
    private String taxonId;
    private String geneLiterature;
    private String geneLiteratureUrl;
    private String geneSynopsis;
    private String geneSynopsisUrl;
    private String dataProvider;
    private String name;

    //private Date dateProduced;
    private String description;
    private String symbol;
    private String geneticEntityExternalUrl;


    private Entity createdBy;
    private SOTerm sOTerm;

    @Relationship(type = "FROM_SPECIES")
    private Species species;

    @Relationship(type = "ALSO_KNOWN_AS")
    private Set<Synonym> synonyms = new HashSet<>();

    @Relationship(type = "ALSO_KNOWN_AS")
    private Set<SecondaryId> secondaryIds = new HashSet<>();

    @Relationship(type = "ALSO_KNOWN_AS")
    private Set<ExternalId> externalIds = new HashSet<>();

    @Relationship(type = "ANNOTATED_TO")
    private Set<GOTerm> gOTerms = new HashSet<>();

    @Relationship(type = "IS_IMPLICATED_IN")
    private List<DOTerm> dOTerms;

    @Override
    public int compareTo(Gene gene) {
        if (gene == null)
            return -1;
        if (!species.equals(gene.species))
            return species.compareTo(gene.species);
        if (symbol == null && gene.getSymbol() == null)
            return 0;
        if (symbol == null)
            return 1;
        if (gene.symbol == null)
            return -1;
        return symbol.compareTo(gene.getSymbol());
    }
}
