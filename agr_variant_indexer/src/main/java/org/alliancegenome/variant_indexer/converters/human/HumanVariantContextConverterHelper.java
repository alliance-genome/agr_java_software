package org.alliancegenome.variant_indexer.converters.human;


import java.io.IOException;
import java.util.*;

import org.alliancegenome.variant_indexer.es.model.ClinicalSig;
import org.alliancegenome.variant_indexer.es.model.Evidence;
import org.alliancegenome.variant_indexer.es.model.Polyphen;
import org.alliancegenome.variant_indexer.es.model.TranscriptFeature;
import org.alliancegenome.variant_indexer.es.model.VariantEffect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;

import htsjdk.variant.variantcontext.CommonInfo;
import htsjdk.variant.variantcontext.VariantContext;

public class HumanVariantContextConverterHelper {

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
            String[] array= null;
            try {
                String jsonArray=mapper.writeValueAsString(ctx.getAttribute("CSQ"));
                try {
                    StringTokenizer tokenizer=new StringTokenizer(jsonArray, ",");
                    array=new String[tokenizer.countTokens()];
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
          /*  Map<String, Polyphen> polyphen = mapPolyphen(ctx, index);
            Map<String, String> varPep = mapVarPep(ctx);
            Map<String, List<VariantEffect>> veffects = mapVE(ctx, index);*/

            for (String obj :array) {
                String[] tokens = obj.toString().split("\\|");
                String feature = new String();
                String allele = new String();
                try {
                   // allele = tokens[0];
                    allele = tokens[0].replace("\\[","").trim();
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
                        String impact = tokens[2];
                        f.setImpact(impact);
                    } catch (Exception e) {
                        //   e.printStackTrace();
                    }
                    try {
                        String symbol = tokens[3];
                        f.setSymbol(symbol);
                    } catch (Exception e) {
                        //   e.printStackTrace();
                    }
                    try {
                        String gene = tokens[4];
                        f.setGene(gene);
                    } catch (Exception e) {
                        //   e.printStackTrace();
                    }
                    try {
                        String featureType = tokens[5];
                        f.setFeatureType(featureType);
                    } catch (Exception e) {
                        //   e.printStackTrace();
                    }
                    try {
                        feature = tokens[6];
                        f.setFeature(feature);
                    } catch (Exception e) {
                        //  e.printStackTrace();
                    }
                    try {
                        String biotype = tokens[7];
                        f.setBiotype(biotype);
                    } catch (Exception e) {
                        //  e.printStackTrace();
                    }
                    try {
                        String exon = tokens[8];
                        f.setExon(exon);
                    } catch (Exception e) {
                        //  e.printStackTrace();
                    }
                    try {
                        String intron = tokens[9];
                        f.setIntron(intron);
                    } catch (Exception e) {
                        //  e.printStackTrace();
                    }
                    try {
                        String HGVSc = tokens[10];
                        f.setHGVSc(HGVSc);
                    } catch (Exception e) {
                        //  e.printStackTrace();
                    }
                    try {
                        String HGVSp = tokens[11];
                        f.setHGVSp(HGVSp);
                    } catch (Exception e) {
                        //  e.printStackTrace();
                    }
                    try {
                        String cDNAPostion = tokens[12];
                        f.setCDNAPosition(cDNAPostion);
                    } catch (Exception e) {
                        //  e.printStackTrace();
                    }
                    try {
                        String CDSPosition = tokens[13];
                        f.setCDSPosition(CDSPosition);
                    } catch (Exception e) {
                        //  e.printStackTrace();
                    }
                    try {
                        String proteinPostion = tokens[14];
                        f.setProteinPosition(proteinPostion);
                    } catch (Exception e) {
                        //  e.printStackTrace();
                    }

                    try {
                        String aminoAcids = tokens[15];
                        f.setAminoAcids(aminoAcids);
                    } catch (Exception e) {
                        // e.printStackTrace();
                    }
                    try {
                        String codon = tokens[16];
                        f.setCodon(codon);
                    } catch (Exception e) {
                        //  e.printStackTrace();
                    }
                    try {
                        String existingVariation = tokens[17];
                        f.setExistingVariation(existingVariation);
                    } catch (Exception e) {
                        //  e.printStackTrace();
                    }
                    try {
                        String distance = tokens[18];
                        f.setDistance(distance);
                    } catch (Exception e) {
                        //  e.printStackTrace();
                    }
                    try {
                        String strand = tokens[19];
                        f.setStrand(strand);
                    } catch (Exception e) {
                        //  e.printStackTrace();
                    }
                    try {
                        String flags = tokens[20];
                        f.setFlags(flags);
                    } catch (Exception e) {
                        //  e.printStackTrace();
                    }
                    try {
                        String symbolSource = tokens[21];
                        f.setSymbolSource(symbolSource);
                    } catch (Exception e) {
                        //  e.printStackTrace();
                    }
                    try {
                        String HGNCId = tokens[22];
                        f.setHGNCId(HGNCId);
                    } catch (Exception e) {
                        //  e.printStackTrace();
                    }
                    try {
                        String source = tokens[23];
                        f.setSource(source);
                    } catch (Exception e) {
                        //  e.printStackTrace();
                    }
                    try {
                        String HGVSOffset = tokens[24];
                        f.setHGVSOffset(HGVSOffset);
                    } catch (Exception e) {
                        //  e.printStackTrace();
                    }
                    try {
                        String HGVSg = tokens[25];
                        f.setHGVSg(HGVSg);
                    } catch (Exception e) {
                        //  e.printStackTrace();
                    }
                    try {
                        String polyphen = tokens[26];
                        f.setPolyphen(polyphen);
                    } catch (Exception e) {
                        //  e.printStackTrace();
                    }
                    try {
                        String sift = tokens[27];
                        f.setSift(sift);
                    } catch (Exception e) {
                        //e.printStackTrace();
                    }

                    features.add(f);
                }
            }
        }
        return features;
    }
    public List<TranscriptFeature> getHumanConsequences(VariantContext ctx, int index, String varNuc) throws JsonProcessingException {
        List<TranscriptFeature> features = new ArrayList<>();
        if(ctx.getAttribute("CSQ")!=null) {
            ObjectMapper mapper=new ObjectMapper();
            String[] array= null;
            try {
                String jsonArray=mapper.writeValueAsString(ctx.getAttribute("CSQ"));
                try {
                    StringTokenizer tokenizer=new StringTokenizer(jsonArray, ",");
                    array=new String[tokenizer.countTokens()];
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
          /*  Map<String, Polyphen> polyphen = mapPolyphen(ctx, index);
            Map<String, String> varPep = mapVarPep(ctx);
            Map<String, List<VariantEffect>> veffects = mapVE(ctx, index);*/

            for (String obj :array) {
                String[] tokens = obj.toString().split("\\|");
                String feature = new String();
                String allele = new String();
                try {
                    // allele = tokens[0];
                    allele = tokens[0].replace("\\[","").trim();
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
                        String impact = tokens[2];
                        f.setImpact(impact);
                    } catch (Exception e) {
                        //   e.printStackTrace();
                    }
                    try {
                        String symbol = tokens[3];
                        f.setSymbol(symbol);
                    } catch (Exception e) {
                        //   e.printStackTrace();
                    }
                    try {
                        String gene = tokens[4];
                        f.setGene(gene);
                    } catch (Exception e) {
                        //   e.printStackTrace();
                    }
                    try {
                        String featureType = tokens[5];
                        f.setFeatureType(featureType);
                    } catch (Exception e) {
                        //   e.printStackTrace();
                    }
                    try {
                        feature = tokens[6];
                        f.setFeature(feature);
                    } catch (Exception e) {
                        //  e.printStackTrace();
                    }
                    try {
                        String biotype = tokens[7];
                        f.setBiotype(biotype);
                    } catch (Exception e) {
                        //  e.printStackTrace();
                    }
                    try {
                        String exon = tokens[8];
                        f.setExon(exon);
                    } catch (Exception e) {
                        //  e.printStackTrace();
                    }
                    try {
                        String intron = tokens[9];
                        f.setIntron(intron);
                    } catch (Exception e) {
                        //  e.printStackTrace();
                    }
                    try {
                        String HGVSc = tokens[10];
                        f.setHGVSc(HGVSc);
                    } catch (Exception e) {
                        //  e.printStackTrace();
                    }
                    try {
                        String HGVSp = tokens[11];
                        f.setHGVSp(HGVSp);
                    } catch (Exception e) {
                        //  e.printStackTrace();
                    }
                    try {
                        String cDNAPostion = tokens[12];
                        f.setCDNAPosition(cDNAPostion);
                    } catch (Exception e) {
                        //  e.printStackTrace();
                    }
                    try {
                        String CDSPosition = tokens[13];
                        f.setCDSPosition(CDSPosition);
                    } catch (Exception e) {
                        //  e.printStackTrace();
                    }
                    try {
                        String proteinPostion = tokens[14];
                        f.setProteinPosition(proteinPostion);
                    } catch (Exception e) {
                        //  e.printStackTrace();
                    }

                    try {
                        String aminoAcids = tokens[15];
                        f.setAminoAcids(aminoAcids);
                    } catch (Exception e) {
                        // e.printStackTrace();
                    }
                    try {
                        String codon = tokens[16];
                        f.setCodon(codon);
                    } catch (Exception e) {
                        //  e.printStackTrace();
                    }
                    try {
                        String existingVariation = tokens[17];
                        f.setExistingVariation(existingVariation);
                    } catch (Exception e) {
                        //  e.printStackTrace();
                    }
                    try {
                        String distance = tokens[18];
                        f.setDistance(distance);
                    } catch (Exception e) {
                        //  e.printStackTrace();
                    }
                    try {
                        String strand = tokens[19];
                        f.setStrand(strand);
                    } catch (Exception e) {
                        //  e.printStackTrace();
                    }
                    try {
                        String flags = tokens[20];
                        f.setFlags(flags);
                    } catch (Exception e) {
                        //  e.printStackTrace();
                    }
                    try {
                        String symbolSource = tokens[21];
                        f.setSymbolSource(symbolSource);
                    } catch (Exception e) {
                        //  e.printStackTrace();
                    }
                    try {
                        String HGNCId = tokens[22];
                        f.setHGNCId(HGNCId);
                    } catch (Exception e) {
                        //  e.printStackTrace();
                    }
                    //REFSEQ_MATCH|SOURCE|REFSEQ_OFFSET|GIVEN_REF|USED_REF|BAM_EDIT|SIFT|PolyPhen|HGVS_OFFSET|HGVSg">
                    try {
                        String refSeqMatch = tokens[23];
                        f.setRefSeqMatch(refSeqMatch);
                    } catch (Exception e) {
                        //  e.printStackTrace();
                    }
                    try {
                        String source = tokens[24];
                        f.setSource(source);
                    } catch (Exception e) {
                        //  e.printStackTrace();
                    }
                    try {
                        String refSeqOffset = tokens[25];
                        f.setRefSeqOffset(refSeqOffset);
                    } catch (Exception e) {
                        //  e.printStackTrace();
                    }
                    try {
                        String givenRef = tokens[26];
                        f.setGivenRef(givenRef);
                    } catch (Exception e) {
                        //  e.printStackTrace();
                    }
                    try {
                        String usedRef = tokens[27];
                        f.setUsedRef(usedRef);
                    } catch (Exception e) {
                        //  e.printStackTrace();
                    }
                    try {
                        String bamEdit = tokens[28];
                        f.setBAMEdit(bamEdit);
                    } catch (Exception e) {
                        //  e.printStackTrace();
                    }
                    try {
                        String polyphen = tokens[29];
                        f.setPolyphen(polyphen);
                    } catch (Exception e) {
                        //  e.printStackTrace();
                    }
                    try {
                        String sift = tokens[30];
                        f.setSift(sift);
                    } catch (Exception e) {
                        //e.printStackTrace();
                    }
                    try {
                        String HGVSOffset = tokens[31];
                        f.setHGVSOffset(HGVSOffset);
                    } catch (Exception e) {
                        //  e.printStackTrace();
                    }
                    try {
                        String HGVSg = tokens[32];
                        f.setHGVSg(HGVSg);
                    } catch (Exception e) {
                        //  e.printStackTrace();
                    }


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

