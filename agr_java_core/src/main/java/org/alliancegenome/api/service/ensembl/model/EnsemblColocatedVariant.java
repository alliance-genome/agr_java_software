package org.alliancegenome.api.service.ensembl.model;

import java.math.BigDecimal;
import java.util.Map;

import org.alliancegenome.neo4j.view.View;

import com.fasterxml.jackson.annotation.JsonView;

import lombok.Data;
import lombok.ToString;

@Data @ToString
public class EnsemblColocatedVariant {

    @JsonView(value = {View.Default.class})
    private String id;
    @JsonView(value = {View.Default.class})
    private Integer strand;
    @JsonView(value = {View.Default.class})
    private Integer phenotype_or_disease;
    @JsonView(value = {View.Default.class})
    private Long start;
    @JsonView(value = {View.Default.class})
    private Long end;
    @JsonView(value = {View.Default.class})
    private String allele_string;
    @JsonView(value = {View.Default.class})
    private String seq_region_name;
    @JsonView(value = {View.Default.class})
    private String minor_allele;
    @JsonView(value = {View.Default.class})
    private BigDecimal minor_allele_freq;
    @JsonView(value = {View.Default.class})
    private Map<String, EnsemblVariantAlleleFreq> frequencies;

}
