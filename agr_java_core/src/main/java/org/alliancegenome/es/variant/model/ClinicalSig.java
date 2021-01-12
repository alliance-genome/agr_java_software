package org.alliancegenome.es.variant.model;

import java.util.HashMap;
import java.util.Map;

public class ClinicalSig {
    public static Map<String, String> csmap;
    static {
        csmap = new HashMap<>();
        csmap.put("CLIN_risk_factor", "risk factor");
        csmap.put("CLIN_protective", "protective");
        csmap.put("CLIN_confers_sensitivity", "confers sensitivity");
        csmap.put("CLIN_other", "other");
        csmap.put("CLIN_drug_response", "drug response");
        csmap.put("CLIN_uncertain_significance", "uncertain significance");
        csmap.put("CLIN_benign", "benign");
        csmap.put("CLIN_likely_pathogenic", "likely pathogenic");
        csmap.put("CLIN_pathogenic", "pathogenic");
        csmap.put("CLIN_likely_benign", "likely benign");
        csmap.put("CLIN_histocompatibility", "histocompatibility");
        csmap.put("CLIN_not_provided", "not provided");
        csmap.put("CLIN_association", "association");
    }
}
