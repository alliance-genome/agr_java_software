package org.alliancegenome.core.translators.tdf;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class TransgenicAlleleDownloadRow {

    private String species;
    private String alleleID;
    private String alleleSymbol;
    private String tgConstructID;
    private String transgenicConstruct;
    private String expGeneID;
    private String expressedGene;
    private String targetID;
    private String knockdownTarget;
    private String regulatoryRegionID;
    private String regulatoryRegion;
    private String hasPhenotype;
    private String hasDisease;

}
