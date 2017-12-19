package org.alliancegenome.indexer.entity.node;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.alliancegenome.indexer.entity.Neo4jEntity;
import org.alliancegenome.indexer.util.DateConverter;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;
import org.neo4j.ogm.annotation.typeconversion.Convert;

import lombok.Getter;
import lombok.Setter;

@NodeEntity
@Getter
@Setter
public class Allele extends Neo4jEntity {

    private String primaryKey;
    private String name;
    private String symbol;
    private String description;

    @Convert(value=DateConverter.class)
    private Date dateProduced;

    @Relationship(type = "ALSO_KNOWN_AS")
    private Set<Synonym> synonyms = new HashSet<>();

    @Relationship(type = "ALSO_KNOWN_AS")
    private Set<SecondaryId> secondaryIds = new HashSet<>();


}
