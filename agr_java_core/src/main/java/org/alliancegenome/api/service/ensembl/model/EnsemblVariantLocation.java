package org.alliancegenome.api.service.ensembl.model;

import lombok.Data;
import lombok.ToString;

@Data @ToString
public class EnsemblVariantLocation {

    private String location;
    private Long start;
    private String assembly_name;
    private String seq_region_name;
    private Integer strand;
    private String ancestral_allele;
    private String coord_system;
    private Long end;
    private String allele_string;
}
