package org.alliancegenome.core.translators.tdf;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class VariantDownloadRow {

    private String symbol;
    private String variantType;
    private String variantSynonyms;
    private String overlaps;
    private String chrPosition;
    private String consequence;
    private String change;
    private String hgvsG;
    private String hgvsC;
    private String hgvsP;
    private String notes;
    private String reference;
    private String crossReference;

}
