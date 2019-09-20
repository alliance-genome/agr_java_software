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

import java.util.Date;

@NodeEntity(label = "Variant")
@Getter
@Setter
public class Variant extends Neo4jEntity implements Comparable<Variant> {

    protected GeneticEntity.CrossReferenceType crossReferenceType;

    @JsonView({View.Default.class, View.API.class})
    @JsonProperty(value = "id")
    protected String primaryKey;

    @JsonView({View.Default.class, View.API.class})
    @JsonProperty(value = "displayName")
    protected String hgvsNomenclature;
    @JsonView({View.Default.class, View.API.class})
    @JsonProperty(value = "consequence")
    protected String geneLevelConsequence;
    private String dataProvider;
    private String genomicReferenceSequence;
    private String genomicVariantSequence;

    @Convert(value = DateConverter.class)
    private Date dateProduced;
    private String release;

    @JsonView({View.Default.class, View.API.class})
    @Relationship(type = "VARIATION_TYPE")
    private SOTerm type;

    @JsonView({View.Default.class, View.API.class})
    @Relationship(type = "ASSOCIATION")
    private GenomicLocation location;

    @Override
    public int compareTo(Variant o) {
        return 0;
    }

    public String getName() {
        return hgvsNomenclature;
    }
}
