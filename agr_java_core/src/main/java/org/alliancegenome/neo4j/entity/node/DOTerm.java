package org.alliancegenome.neo4j.entity.node;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.Getter;
import lombok.Setter;
import org.alliancegenome.es.util.DateConverter;
import org.alliancegenome.neo4j.entity.Neo4jEntity;
import org.alliancegenome.neo4j.view.View;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;
import org.neo4j.ogm.annotation.typeconversion.Convert;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@NodeEntity
@Getter
@Setter
public class DOTerm extends Neo4jEntity {

    public static final String HIGH_LEVEL_TERM_LIST_SLIM = "DO_AGR_slim";

    @JsonView({View.Default.class})
    private String doUrl;
    private String doDisplayId;
    private String doId;
    private String doPrefix;
    @JsonView({View.API.class})
    @JsonProperty(value="id")
    private String primaryKey;
    @JsonView({View.API.class})
    private String name;
    private String definition;
    private List<String> defLinks;
    private List<String> subset;

    private String nameKey;
    private String is_obsolete;
    
    @Convert(value=DateConverter.class)
    private Date dateProduced;
    
    private String zfinLink;
    private String humanLink;
    private String rgdLink;
    private String sgdLink;
    private String ratOnlyRgdLink;
    private String humanOnlyRgdLink;
    private String wormbaseLink;
    private String flybaseLink;
    private String mgiLink;

    private List<DOTerm> highLevelTermList = new ArrayList<>(2);

    @Relationship(type = "IS_IMPLICATED_IN", direction = Relationship.INCOMING)
    private List<Gene> genes;

    @Relationship(type = "ASSOCIATION", direction = Relationship.INCOMING)
    private List<DiseaseEntityJoin> diseaseEntityJoins;

    @Relationship(type = "IS_A", direction = Relationship.OUTGOING)
    private List<DOTerm> parents;

    @Relationship(type = "IS_A", direction = Relationship.INCOMING)
    private List<DOTerm> children;

    @Relationship(type = "ALSO_KNOWN_AS")
    private List<Synonym> synonyms;

    @Relationship(type = "CROSS_REFERENCE")
    private List<CrossReference> crossReferences;

    @Override
    public String toString() {
        return primaryKey + ":"+name;
    }
}
