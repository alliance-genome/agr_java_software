package org.alliancegenome.api.service.ensembl.model;

import java.math.BigDecimal;
import java.util.Map;

import lombok.Data;
import lombok.ToString;

@Data @ToString
public class EnsemblColocatedVariant {

    private String id;
    private Integer strand;
    private Integer phenotype_or_disease;
    private Long start;
    private Long end;
    private String allele_string;
    private String seq_region_name;
    private String minor_allele;
    private BigDecimal minor_allele_freq;
    private Map<String, EnsemblVariantAlleleFreq> frequencies;

}
