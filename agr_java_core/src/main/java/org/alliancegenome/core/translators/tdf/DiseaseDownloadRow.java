package org.alliancegenome.core.translators.tdf;

import lombok.*;

@Setter
@Getter
public class DiseaseDownloadRow {

    private String mainEntityID;
    private String mainEntitySymbol;
    private String entityType;

    private String geneticEntityID;
    private String geneticEntityName;
    private String geneticEntityType;
    private String speciesID;
    private String speciesName;
    private String association;
    private String diseaseID;
    private String diseaseName;
    private String basedOnID;
    private String basedOnName;
    private String evidenceCode;
    private String evidenceCodeName;
    private String source;
    private String reference;
    private String dateAssigned;
}
