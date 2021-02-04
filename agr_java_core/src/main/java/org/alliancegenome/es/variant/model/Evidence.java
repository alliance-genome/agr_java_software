package org.alliancegenome.es.variant.model;

import java.util.*;

public class Evidence {
    public static Map<String, String> emap;
    static {
        emap=new HashMap<>();
        emap.put("E_Freq","Frequency");
        emap.put("E_TOPMed","TOPMed");
        emap.put("E_gnomAD","gnomAD");
        emap.put("E_1000G","1000Genomes");
        emap.put("dbSNP_153","dbSNP");
        emap.put("COSMIC_90","COSMIC catalogue");
        emap.put("ClinVar_201912","ClinVar");
        emap.put("HGMD-PUBLIC_20194","HGMD-PUBLIC dataset December 2019");
        emap.put("ESP_20141103","NHLBI ESP version v.0.0.30");
        emap.put("E_Cited","Cited");
        emap.put("E_Multiple_observations","E_Multiple_observations");
        emap.put("E_Hapmap","HapMap");
        emap.put("E_Phenotype_or_Disease","Phenotype_or_Disease");
        emap.put("E_ESP","ESP");
        emap.put("E_ExAC","ExAC");
    }
}
