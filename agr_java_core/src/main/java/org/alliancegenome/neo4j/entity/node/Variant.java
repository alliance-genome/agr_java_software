package org.alliancegenome.neo4j.entity.node;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.alliancegenome.es.util.DateConverter;
import org.alliancegenome.neo4j.entity.Neo4jEntity;
import org.alliancegenome.neo4j.entity.relationship.GenomeLocation;
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

    private String dataProvider;
    @JsonView({View.API.class})
    private String genomicReferenceSequence;
    @JsonView({View.API.class})
    private String genomicVariantSequence;

    private String paddingLeft = "";
    private String paddingRight = "";

    @Convert(value = DateConverter.class)
    private Date dateProduced;
    private String release;

    @JsonView({View.Default.class, View.API.class})
    @Relationship(type = "VARIATION_TYPE")
    private SOTerm type;

    @JsonView({View.VariantAPI.class})
    @Relationship(type = "COMPUTED_GENE", direction = Relationship.INCOMING)
    private Gene gene;

    @Relationship(type = "ASSOCIATION")
    protected GeneLevelConsequence geneLevelConsequence;

    @JsonView({View.Default.class, View.API.class})
    @Relationship(type = "ASSOCIATION")
    private GenomeLocation location;

    @JsonView({View.Default.class, View.API.class})
    @JsonProperty(value = "consequence")
    public String getConsequence() {
        return geneLevelConsequence != null ? geneLevelConsequence.getGeneLevelConsequence() : null;
    }

    @JsonProperty(value = "consequence")
    public void setConsequence(String consequence) {
        if (geneLevelConsequence != null)
            return;
        GeneLevelConsequence co = new GeneLevelConsequence();
        co.setGeneLevelConsequence(consequence);
        this.geneLevelConsequence = co;
    }

    @Override
    public int compareTo(Variant o) {
        return 0;
    }

    public String getName() {
        return hgvsNomenclature;
    }

    @JsonProperty(value = "nucleotideChange")
    public void setNucleotideChange(String consequence) {
        // ignore as this is always calculated
    }

    public String getPaddingLeft() {
        return paddingLeft.toLowerCase();
    }

    @JsonView({View.Default.class, View.API.class})
    @JsonProperty(value = "nucleotideChange")
    public String getNucleotideChange() {
        String change = "";
        if (type.isInsertion() || type.isDeletion()) {
            change += getPaddedChange(getGenomicReferenceSequence());
            change += ">";
            change += getPaddedChange(getGenomicVariantSequence());
        } else {
            change += getGenomicReferenceSequence();
            change += ">";
            change += getGenomicVariantSequence();
        }
        return change;
    }

    private String getPaddedChange(String change) {
        return (getPaddingLeft().charAt(getPaddingLeft().length() - 1) + change);
    }
}
