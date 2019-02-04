package org.alliancegenome.neo4j.view;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class PhenotypeFilter extends BaseFilter {

    private String phenotype;

}