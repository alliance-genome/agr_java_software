package org.alliancegenome.api.service.ensembl.model;

import java.util.List;

import org.alliancegenome.neo4j.view.View;

import com.fasterxml.jackson.annotation.JsonView;

import lombok.Data;
import lombok.ToString;

@Data @ToString
public class EnsemblVariant {

    @JsonView({View.Default.class})
    private String id;
    @JsonView({View.Default.class})
    private Long start;
    @JsonView({View.Default.class})
    private Long end;
    @JsonView({View.Default.class})
    private Integer strand;
    @JsonView({View.Default.class})
    private String source;
    @JsonView({View.Default.class})
    private String assembly_name;
    @JsonView({View.Default.class})
    private String consequence_type;
    @JsonView({View.Default.class})
    private String feature_type;
    @JsonView({View.Default.class})
    private Integer seq_region_name;
    @JsonView({View.Default.class})
    private List<String> clinical_significance;
    @JsonView({View.Default.class})
    private List<String> alleles;
}
