package org.alliancegenome.core.translators.tdf;

import lombok.*;

@Setter
@Getter
public class AlleleDownloadRow {

    private String alleleID;
    private String alleleSymbol;
    private String alleleSynonyms;
    private String variantCategory;
    private String variantSymbol;
    private String variantType;
    private String variantConsequence;
    private String reference;
    private String hasPhenotype;
    private String hasDisease;

}
