package org.alliancegenome.api.service.ensembl.model;

import java.util.List;

import org.alliancegenome.neo4j.view.View;

import com.fasterxml.jackson.annotation.JsonView;

import lombok.Data;
import lombok.ToString;

@Data @ToString
public class EnsemblVariantConsequence {

    @JsonView(value = {View.Default.class})
    private String id;
    @JsonView(value = {View.Default.class})
    private String input;
    @JsonView(value = {View.Default.class})
    private String assembly;
    @JsonView(value = {View.Default.class})
    private String assembly_name;
    @JsonView(value = {View.Default.class})
    private String allele_string;
    @JsonView(value = {View.Default.class})
    private Long start;
    @JsonView(value = {View.Default.class})
    private Long end;
    @JsonView(value = {View.Default.class})
    private String most_severe_consequence;
    @JsonView(value = {View.Default.class})
    private String seq_region_name;
    @JsonView(value = {View.Default.class})
    private String strand;
    @JsonView(value = {View.Default.class})
    private List<EnsemblColocatedVariant> colocated_variants;
    @JsonView(value = {View.Default.class})
    private List<EnsemblVariantTranscriptConsequence> transcript_consequences;

}
