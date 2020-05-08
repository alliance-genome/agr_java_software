package org.alliancegenome.api.service.ensembl.model;

import lombok.Data;
import lombok.ToString;

@Data @ToString
public class EnsemblVariantGenotype {

    private String gender;
    private String genotype;
    private String sample;
}
