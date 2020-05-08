package org.alliancegenome.api.service.ensembl.model;

import java.math.BigDecimal;
import java.util.List;

import lombok.Data;
import lombok.ToString;

@Data @ToString
public class EnsemblVariantTranscriptConsequence {

    private String gene_id;
    private String gene_symbol;
    private String codons;
    private String hgnc_id;
    private String impact;
    private Integer strand;
    private Integer sift_score;
    private String sift_prediction;
    private String gene_symbol_source;
    private BigDecimal polyphen_score;
    private String polyphen_prediction;
    private Integer protein_start;
    private Integer protein_end;
    private Integer cds_start;
    private Integer cds_end;
    private Integer cdna_start;
    private Integer cdna_end;
    private String biotype;
    private String amino_acids;
    private String transcript_id;
    private String variant_allele;
    private Integer distance;
    private List<String> consequence_terms;

}
