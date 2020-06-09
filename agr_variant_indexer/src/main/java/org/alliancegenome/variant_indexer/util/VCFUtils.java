package org.alliancegenome.variant_indexer.util;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.alliancegenome.variant_indexer.model.ClinicalSig;
import org.alliancegenome.variant_indexer.model.Evidence;
import org.alliancegenome.variant_indexer.model.Polyphen;
import org.alliancegenome.variant_indexer.model.TranscriptFeature;
import org.alliancegenome.variant_indexer.model.VariantEffect;

import com.fasterxml.jackson.core.JsonProcessingException;

import htsjdk.variant.variantcontext.CommonInfo;
import htsjdk.variant.variantcontext.VariantContext;

public class VCFUtils {

    public List<String> mapEvidence(VariantContext ctx){
        CommonInfo info = ctx.getCommonInfo();
        List<String> evidences = new ArrayList<>();
        String key;
        String value;
        boolean evidence;
        for (Map.Entry e: Evidence.emap.entrySet()) {
            key = (String) e.getKey();
            value = (String) e.getValue();
            if(info.getAttribute(key) != null) {
                evidence = (boolean) info.getAttribute(key);
                if (evidence) {
                    evidences.add(value);
                }
            }
        }
        return evidences;
    }
    public List<String> mapClinicalSignificance(VariantContext ctx){
        CommonInfo info = ctx.getCommonInfo();
        List<String> significance = new ArrayList<>();
        String key;
        String value;
        boolean flag;
        for (Map.Entry e: ClinicalSig.csmap.entrySet() ) {
            key = (String) e.getKey();
            value = (String) e.getValue();
            if(info.getAttribute(key)!=null) {
                flag = (boolean) info.getAttribute(key);
                if (flag) {
                    significance.add(value);
                }
            }
        }
        return significance;
    }
    public List<TranscriptFeature> getConsequences(VariantContext ctx, int index, String varNuc) throws JsonProcessingException {
        List<TranscriptFeature> features = new ArrayList<>();
        if(ctx.getAttribute("CSQ")!=null) {
            ObjectMapper mapper=new ObjectMapper();
            String[] array= new String[0];
            try {
                String jsonArray=mapper.writeValueAsString(ctx.getAttribute("CSQ"));
                try {
                    array = mapper.readValue(jsonArray, String[].class);
                } catch (IOException e) {
                    try {
                        array=mapper.readValue(Arrays.asList(jsonArray).toString(), String[].class);
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
            Map<String, Polyphen> polyphen = mapPolyphen(ctx, index);
            Map<String, String> varPep = mapVarPep(ctx);
            Map<String, List<VariantEffect>> veffects = mapVE(ctx, index);

            for (String obj :array) {
                String[] tokens = obj.toString().split("\\|");
                String feature = new String();
                String allele = new String();
                try {
                   // allele = tokens[0];
                    allele = tokens[0].trim();
                } catch (Exception e) {
                }
                if (allele.equalsIgnoreCase(varNuc)) {
                    TranscriptFeature f = new TranscriptFeature();
                    f.setAllele(allele);
                    try {
                        String consequence = tokens[1];
                        f.setConsequence(consequence);
                    } catch (Exception e) {
                    }
                    try {
                        String featureType = tokens[2];
                        f.setFeatureType(featureType);
                    } catch (Exception e) {
                        //   e.printStackTrace();
                    }
                    try {
                        feature = tokens[3];
                        f.setFeature(feature);
                    } catch (Exception e) {
                        //  e.printStackTrace();
                    }
                    try {
                        String aminoAcids = tokens[4];
                        f.setAminoAcids(aminoAcids);
                    } catch (Exception e) {
                        // e.printStackTrace();
                    }
                    try {
                        String sift = tokens[5];
                        f.setSift(sift);
                    } catch (Exception e) {
                        //e.printStackTrace();
                    }

                    f.setPolyphen(polyphen.get(feature));
                    f.setVarPep(varPep.get(feature));
                    f.setVariantEffects(veffects.get(feature));

                    features.add(f);
                }
            }
        }
        return features;
    }
    public Map<String, List<VariantEffect>> mapVE(VariantContext ctx, int index){
        Map<String, List<VariantEffect>> veMap=new HashMap<>();
        if(ctx.getAttribute("VE")!=null) {
            ObjectMapper mapper=new ObjectMapper();
            String[] array= new String[0];
            try {
                String jsonArray=mapper.writeValueAsString(ctx.getAttribute("VE"));
                try {
                    array = mapper.readValue(jsonArray, String[].class);
                } catch (IOException e) {
                    try {
                        array=mapper.readValue(Arrays.asList(jsonArray).toString(), String[].class);
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }               List<String> ids=new ArrayList<>();
            for (String obj : array) {
             //   System.out.println(obj.toString() + "\n");
                String[] tokens = obj.toString().split("\\|");

                    try {
                        if (Integer.parseInt(tokens[1]) == index) {
                        String id = tokens[3];
                        List<VariantEffect> ves = new ArrayList<>();
                        if (!ids.contains(id)) {

                            ids.add(id);
                            VariantEffect ve = new VariantEffect();
                            ve.setConsequence(tokens[0]);
                            ve.setFeatureType(tokens[2]);
                            ves.add(ve);
                            veMap.put(id, ves);
                        } else {
                            ves = veMap.get(id);
                            if (ves == null) {
                                ves = new ArrayList<>();
                            }
                            VariantEffect ve = new VariantEffect();
                            ve.setConsequence(tokens[0]);
                            ve.setFeatureType(tokens[2]);
                            ves.add(ve);
                            veMap.put(id, ves);
                        }
                    }
                    } catch (Exception e) {
                    }

            }
        }
        return veMap;
    }
    public Map<String, Polyphen> mapPolyphen(VariantContext ctx, int index){
        Map<String, Polyphen> polyphenMap=new HashMap<>();
        if(ctx.getAttribute("Polyphen")!=null) {
            ObjectMapper mapper=new ObjectMapper();
            String[] array= new String[0];
            try {
                String jsonArray=mapper.writeValueAsString(ctx.getAttribute("Polyphen"));
                try {
                    array = mapper.readValue(jsonArray, String[].class);
                } catch (IOException e) {
                    try {
                        array=mapper.readValue(Arrays.asList(jsonArray).toString(), String[].class);
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                    //  e.printStackTrace();
                }
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }

            for (String obj : array) {
                Polyphen p = new Polyphen();
                //.out.println(obj.toString() + "\n");
                String[] tokens = obj.toString().split("\\|");
                try {
                    p.setPrediction(tokens[1]);
                    p.setValue(tokens[2]);
                    polyphenMap.put(tokens[3], p);
                } catch (Exception e) {
                }
            }
        }
        return polyphenMap;
    }
    public Map<String, String> mapVarPep(VariantContext ctx){
        Map<String, String> varPep=new HashMap<>();
        if(ctx.getAttribute("VarPep")!=null) {
            ObjectMapper mapper=new ObjectMapper();
            String[] array= new String[0];
            try {
                String jsonArray=mapper.writeValueAsString(ctx.getAttribute("VarPep"));
                try {
                    array = mapper.readValue(jsonArray, String[].class);
                } catch (IOException e) {
                    try {
                        array=mapper.readValue(Arrays.asList(jsonArray).toString(), String[].class);
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                    //  e.printStackTrace();
                }
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }            for (String obj : array) {
           //     System.out.println(obj.toString() + "\n");
                String[] tokens = obj.toString().split("\\|");
                try {
                    varPep.put(tokens[2], tokens[1]);
                } catch (Exception e) {
                }
            }
        }
        return varPep;
    }
    public  boolean alleleIsValid(String allele) {
        for( int i=0; i<allele.length(); i++ ) {
            char c = allele.charAt(i);
            if( c=='A' || c=='C' || c=='G' || c=='T' || c=='N' || c=='-' )
                continue;
            return false;
        }
        return true;
    }
}

