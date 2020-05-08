package org.alliancegenome.api.service.ensembl.model;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.ToString;

@Data @ToString
public class EnsemblVariantFeature {

    private String minor_allele;
    private String name;
    private String source;
    private String var_class;
    @JsonProperty("MAF")
    private BigDecimal maf;
    private String ambiguity;
    private String most_severe_consequence;
    
    private List<String> evidence;
    private List<String> synonyms;
    private List<EnsemblVariantGenotype> genotypes;
    private List<EnsemblVariantLocation> mappings;
}
