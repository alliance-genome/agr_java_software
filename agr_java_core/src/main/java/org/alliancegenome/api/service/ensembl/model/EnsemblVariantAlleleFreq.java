package org.alliancegenome.api.service.ensembl.model;

import java.math.BigDecimal;

import org.alliancegenome.neo4j.view.View;

import com.fasterxml.jackson.annotation.JsonView;

import lombok.Data;
import lombok.ToString;

@Data @ToString
public class EnsemblVariantAlleleFreq {

    @JsonView(value = {View.Default.class})
    private BigDecimal aa;
    @JsonView(value = {View.Default.class})
    private BigDecimal gnomad_oth;
    @JsonView(value = {View.Default.class})
    private BigDecimal amr;
    @JsonView(value = {View.Default.class})
    private BigDecimal gnomad_fin;
    @JsonView(value = {View.Default.class})
    private BigDecimal eur;
    @JsonView(value = {View.Default.class})
    private BigDecimal eas;
    @JsonView(value = {View.Default.class})
    private BigDecimal sas;
    @JsonView(value = {View.Default.class})
    private BigDecimal gnomad_asj;
    @JsonView(value = {View.Default.class})
    private BigDecimal gnomad_eas;
    @JsonView(value = {View.Default.class})
    private BigDecimal gnomad;
    @JsonView(value = {View.Default.class})
    private BigDecimal gnomad_nfe;
    @JsonView(value = {View.Default.class})
    private BigDecimal ea;
    @JsonView(value = {View.Default.class})
    private BigDecimal gnomad_amr;
    @JsonView(value = {View.Default.class})
    private BigDecimal afr;
    @JsonView(value = {View.Default.class})
    private BigDecimal gnomad_sas;
    @JsonView(value = {View.Default.class})
    private BigDecimal gnomad_afr;

}
