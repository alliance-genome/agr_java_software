package org.alliancegenome.neo4j.entity.node;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.Getter;
import lombok.Setter;
import org.alliancegenome.es.util.DateConverter;
import org.alliancegenome.neo4j.entity.relationship.GenomeLocation;
import org.alliancegenome.neo4j.view.View;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;
import org.neo4j.ogm.annotation.typeconversion.Convert;

import java.util.*;
import java.util.stream.Collectors;

@NodeEntity(label = "Variant")
@Getter
@Setter
@Schema(name = "Variant", description = "POJO that represents the Variant")
public class Variant extends GeneticEntity implements Comparable<Variant> {

    public Variant() {
        this.crossReferenceType = CrossReferenceType.ALLELE;
    }

    @JsonView({View.Default.class, View.API.class})
    @JsonProperty(value = "displayName")
    private String hgvsNomenclature;

    @JsonView({View.Default.class, View.API.class})
    private String name;

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
    private SOTerm variantType;

    @JsonView({View.API.class})
    @Relationship(type = "COMPUTED_GENE", direction = Relationship.INCOMING)
    private Gene gene;

    @JsonView({View.GeneAlleleVariantSequenceAPI.class, View.AlleleVariantSequenceConverterForES.class})
    private String start;
    @JsonView({View.GeneAlleleVariantSequenceAPI.class, View.AlleleVariantSequenceConverterForES.class})
    private String end;

    @Relationship(type = "ASSOCIATION")
    protected GeneLevelConsequence geneLevelConsequence;

    @JsonView({View.VariantAPI.class})
    @Relationship(type = "ASSOCIATION")
    protected Set<Note> notes;

    @JsonView({View.API.class})
    @Relationship(type = "ASSOCIATION")
    protected Set<Publication> publications;

    @JsonView({View.API.class})
    @Relationship(type = "ASSOCIATION")
    private GenomeLocation location;

    @JsonView({View.API.class})
    @Relationship(type = "ASSOCIATION", direction = Relationship.INCOMING)
    protected List<Transcript> transcriptList;

    @Relationship(type = "ASSOCIATION")
    protected List<TranscriptLevelConsequence> transcriptLevelConsequence;

    @JsonView({View.API.class})
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

    public String getPaddingLeft() {
        return paddingLeft.toLowerCase();
    }

    private String nucleotideChange;

    @JsonProperty(value = "nucleotideChange")
    public void setNucleotideChange(String change) {
        nucleotideChange = change;
    }


    @JsonView({View.Default.class, View.API.class})
    @JsonProperty(value = "nucleotideChange")
    public String getNucleotideChange() {
        if (StringUtils.isNotEmpty(nucleotideChange))
            return nucleotideChange;
        StringBuilder builder = new StringBuilder();
        if (variantType != null && (variantType.isInsertion() || variantType.isDeletion())) {
            builder.append(getGenomicReferenceSequence());
            builder.append(">");
            builder.append(getPaddedChange(getGenomicVariantSequence()));
            // if no genomic sequence is available add 'N+'
            if (StringUtils.isEmpty(getGenomicReferenceSequence()) &&
                    StringUtils.isEmpty(getGenomicVariantSequence()))
                builder.append("N+");
        } else {
            builder.append(getGenomicReferenceSequence());
            builder.append(">");
            builder.append(getGenomicVariantSequence());
        }
        nucleotideChange = builder.toString();
        return nucleotideChange;
    }

    private String getPaddedChange(String change) {
        if (getPaddingLeft().length() == 0)
            return change;
        return (getPaddingLeft().charAt(getPaddingLeft().length() - 1) + change);
    }

    @JsonView({View.VariantAPI.class})
    public List<String> getHgvsG() {
        List<String> names = new ArrayList<>();
        names.add(name);
        names.add(hgvsNomenclature);
        if(transcriptLevelConsequence != null) {
            if (CollectionUtils.isNotEmpty(transcriptLevelConsequence)) {
                names.addAll(transcriptLevelConsequence.stream()
                        .map(TranscriptLevelConsequence::getHgvsVEPGeneNomenclature)
                        .distinct()
                        .collect(Collectors.toList()));
            }
            names.sort(Comparator.naturalOrder());
        }
        return names;
    }

    @JsonView({View.VariantAPI.class})
    public List<String> getHgvsC() {
        if (CollectionUtils.isNotEmpty(transcriptLevelConsequence)) {
            return transcriptLevelConsequence.stream()
                    .map(TranscriptLevelConsequence::getHgvsCodingNomenclature)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList());
        }
        return null;
    }

    @JsonView({View.VariantAPI.class})
    public List<String> getHgvsP() {
        if (CollectionUtils.isNotEmpty(transcriptLevelConsequence)) {
            return transcriptLevelConsequence.stream()
                    .map(TranscriptLevelConsequence::getHgvsProteinNomenclature)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList());
        }
        return null;
    }
}
