package org.alliancegenome.core.translators.tdf;

import lombok.*;

@Setter
@Getter
public class PhenotypeDownloadRow {

    private String mainEntityID;
    private String mainEntitySymbol;

    private String geneticEntityID;
    private String geneticEntityName;
    private String geneticEntityType;
    private String phenotype;

    private String reference;
    private String source;
}
