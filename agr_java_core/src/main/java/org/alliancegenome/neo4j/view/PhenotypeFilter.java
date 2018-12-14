package org.alliancegenome.neo4j.view;

import lombok.Getter;
import lombok.Setter;
import org.alliancegenome.neo4j.entity.SpeciesType;

import java.util.List;
import java.util.stream.Collectors;

@Setter
@Getter
public class PhenotypeFilter extends BaseFilter {

    private String phenotype;

}