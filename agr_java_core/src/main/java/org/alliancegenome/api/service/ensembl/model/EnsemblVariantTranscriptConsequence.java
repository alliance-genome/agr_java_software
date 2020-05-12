package org.alliancegenome.api.service.ensembl.model;

import java.math.BigDecimal;
import java.util.List;

import org.alliancegenome.neo4j.view.View;

import com.fasterxml.jackson.annotation.JsonView;

import lombok.Data;
import lombok.ToString;

@Data @ToString
public class EnsemblVariantTranscriptConsequence {

    @JsonView(value = {View.Default.class})
    private String gene_id;
    @JsonView(value = {View.Default.class})
    private String gene_symbol;
    @JsonView(value = {View.Default.class})
    private String codons;
    @JsonView(value = {View.Default.class})
    private String hgnc_id;
    @JsonView(value = {View.Default.class})
    private String impact;
    @JsonView(value = {View.Default.class})
    private Integer strand;
    @JsonView(value = {View.Default.class})
    private Integer sift_score;
    @JsonView(value = {View.Default.class})
    private String sift_prediction;
    @JsonView(value = {View.Default.class})
    private String gene_symbol_source;
    @JsonView(value = {View.Default.class})
    private BigDecimal polyphen_score;
    @JsonView(value = {View.Default.class})
    private String polyphen_prediction;
    @JsonView(value = {View.Default.class})
    private Integer protein_start;
    @JsonView(value = {View.Default.class})
    private Integer protein_end;
    @JsonView(value = {View.Default.class})
    private Integer cds_start;
    @JsonView(value = {View.Default.class})
    private Integer cds_end;
    @JsonView(value = {View.Default.class})
    private Integer cdna_start;
    @JsonView(value = {View.Default.class})
    private Integer cdna_end;
    @JsonView(value = {View.Default.class})
    private String biotype;
    @JsonView(value = {View.Default.class})
    private String amino_acids;
    @JsonView(value = {View.Default.class})
    private String transcript_id;
    @JsonView(value = {View.Default.class})
    private String variant_allele;
    @JsonView(value = {View.Default.class})
    private Integer distance;
    @JsonView(value = {View.Default.class})
    private List<String> consequence_terms;

}
