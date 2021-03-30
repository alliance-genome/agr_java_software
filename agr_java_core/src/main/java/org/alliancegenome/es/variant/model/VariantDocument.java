package org.alliancegenome.es.variant.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import org.alliancegenome.es.index.site.document.SearchableItemDocument;

import java.util.List;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class VariantDocument extends SearchableItemDocument {
    
    private String chromosome;
    private int startPos;
    private int endPos;
    private String refNuc;
    private String varNuc;
    private String qual;
    private String documentVariantType;
    private String filter;
    private String refPep;
    private String aa; // ancestral allele
    private String MA; //minor allele
    private double MAF;    // minor allele frequency
    private int MAC;    // minor allele count
    private List<String> evidence;
    private List<String> clinicalSignificance;
    private List<TranscriptFeature> consequences;
    private List<String> samples;
    private String matchedWithHTP;

}
