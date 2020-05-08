package org.alliancegenome.api.service.ensembl.model;

import java.util.List;

import lombok.Data;
import lombok.ToString;

@Data @ToString
public class EnsemblVariantConsequence {

    private String id;
    private String input;
    private String assembly;
    private String assembly_name;
    private String allele_string;
    private Long start;
    private Long end;
    private String most_severe_consequence;
    private String seq_region_name;
    private String strand;
    private List<EnsemblColocatedVariant> colocated_variants;
    private List<EnsemblVariantTranscriptConsequence> transcript_consequences;

}
