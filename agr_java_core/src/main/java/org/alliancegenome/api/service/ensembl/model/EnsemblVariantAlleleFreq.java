package org.alliancegenome.api.service.ensembl.model;

import java.math.BigDecimal;

import lombok.Data;
import lombok.ToString;

@Data @ToString
public class EnsemblVariantAlleleFreq {

    private BigDecimal aa;
    private BigDecimal gnomad_oth;
    private BigDecimal amr;
    private BigDecimal gnomad_fin;
    private BigDecimal eur;
    private BigDecimal eas;
    private BigDecimal sas;
    private BigDecimal gnomad_asj;
    private BigDecimal gnomad_eas;
    private BigDecimal gnomad;
    private BigDecimal gnomad_nfe;
    private BigDecimal ea;
    private BigDecimal gnomad_amr;
    private BigDecimal afr;
    private BigDecimal gnomad_sas;
    private BigDecimal gnomad_afr;

}
