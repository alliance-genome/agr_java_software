package org.alliancegenome.core.variant.converters;

import htsjdk.variant.variantcontext.Allele;
import htsjdk.variant.variantcontext.CommonInfo;
import htsjdk.variant.variantcontext.VariantContext;
import io.github.lukehutch.fastclasspathscanner.utils.Join;
import lombok.extern.log4j.Log4j2;
import org.alliancegenome.es.variant.model.ClinicalSig;
import org.alliancegenome.es.variant.model.Evidence;
import org.alliancegenome.es.variant.model.TranscriptFeature;
import org.alliancegenome.es.variant.model.VariantDocument;
import org.alliancegenome.neo4j.entity.SpeciesType;

import java.util.*;
import java.util.stream.Collectors;

@Log4j2
public class VariantContextConverter {

    public List<VariantDocument> convertVariantContext(VariantContext ctx, SpeciesType speciesType, String[] header) {

        Allele refNuc = ctx.getReference();

        List<VariantDocument> returnDocuments = new ArrayList<>();
        for (Allele a : ctx.getAlternateAlleles()) {
            if (a.compareTo(refNuc) < 0) {
                continue;
            }
            if (!alleleIsValid(ctx.getReference().getBaseString())) {
                //   System.out.println(" *** Ref Nucleotides must be A,C,G,T,N");
                continue;
            }
            if (!alleleIsValid(a.getBaseString())) {
                //     System.out.println(" *** Var Nucleotides must be A,C,G,T,N");
                continue;
            }

            VariantDocument variantDocument = new VariantDocument();
            List<TranscriptFeature> htpConsequences = null;
            try {
                htpConsequences = getConsequences(ctx, a.getBaseString(), header);
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            String hgvsNomenclature = null;
            
            if(htpConsequences != null && htpConsequences.size() > 0) {
                hgvsNomenclature = htpConsequences.get(0).getHgvsg();
                if(hgvsNomenclature == null) {
                    StringBuilder builder = new StringBuilder();
                    builder.append(ctx.getContig());
                    builder.append(":");
                    builder.append(ctx.getStart());
                    builder.append("-needs-real-hgvs");
                    hgvsNomenclature = builder.toString();
                }
            }
            
            variantDocument.setAlterationType("variant");
            variantDocument.setName(hgvsNomenclature);
            variantDocument.setNameKey(hgvsNomenclature);
            Set<String> variantTypes = new HashSet<>();
            try {
                variantDocument.setConsequences(htpConsequences);
            } catch (Exception e) {
                e.printStackTrace();
            }
            String variantType = ctx.getType().name();
            if ("INDEL".equals(variantType)) {
                variantType = "delins";
            }

            variantTypes.add(variantType);
            variantDocument.setVariantType(variantTypes);
            variantDocument.setId(ctx.getID());
            variantDocument.setPrimaryKey(ctx.getID());
            variantDocument.setSpecies(speciesType.getName());
            variantDocument.setChromosome(ctx.getContig());
            variantDocument.setStartPos(ctx.getStart());

            int endPos = 0;

            int start = ctx.getStart();
            if (ctx.isSNP()) {
                endPos = start + 1;
            } // insertions
            if (ctx.isSimpleInsertion()) {
                endPos = start;
                //  System.out.println("INSERTION");
            } else if (ctx.isSimpleDeletion()) {
                endPos = start + refNuc.getDisplayString().length();
                //  System.out.println("Deletion");
            }

            variantDocument.setRefNuc(refNuc.getBaseString());
            variantDocument.setVarNuc(a.getBaseString());
            variantDocument.setEndPos(endPos);
            variantDocument.setDocumentVariantType(ctx.getCommonInfo().getAttributeAsString("TSA", ""));

            HashSet<String> list = new HashSet<String>();
            for(TranscriptFeature x: variantDocument.getConsequences()) {
                StringBuilder builder = new StringBuilder();
                builder.append(x.getSymbol());
                builder.append(" (");
                builder.append(speciesType.getAbbreviation());
                builder.append(")");
                list.add(builder.toString());
            }
            
            variantDocument.setGenes(list);
//                  variantDocument.getConsequences()
//                          .parallelStream()
//                          .map(x -> {
//                              StringBuilder builder = new StringBuilder();
//                              builder.append(x.getSymbol());
//                              builder.append(" (");
//                              builder.append(speciesType.getAbbreviation());
//                              builder.append(")");
//                              return builder.toString();
//                              //return x.getSymbol() + " (" + speciesType.getAbbreviation() + ")";
//                          })
//                          .collect(Collectors.toSet())
//          );
            
            mapAttributes(ctx, variantDocument);
            
            if (ctx.getAttributes().containsKey("MA"))
                variantDocument.setMA(ctx.getAttributeAsString("MA", ""));
            if (ctx.getAttributes().containsKey("MAF"))
                variantDocument.setMAF(ctx.getAttributeAsDouble("MAF", 0.0));
            if (ctx.getAttributes().containsKey("MAC"))
                variantDocument.setMAC(ctx.getAttributeAsInt("MAC", 0));
            if (ctx.getAttributes().containsKey("RefPep"))
                variantDocument.setRefPep(ctx.getAttributeAsString("RefPep", ""));
            if (ctx.getAttributes().containsKey("AA"))
                variantDocument.setAa(ctx.getAttributeAsString("AA", ""));
            if (ctx.getAttributes().containsKey("QUAL"))
                variantDocument.setQual(ctx.getAttributeAsString("QUAL", ""));
            if (ctx.getAttributes().containsKey("FILTER"))
                variantDocument.setQual(ctx.getAttributeAsString("FILTER", ""));
            variantDocument.setMolecularConsequence(
                    variantDocument.getConsequences().stream()
                            .map(TranscriptFeature::getConsequence)
                            .collect(Collectors.toSet())
            );
            returnDocuments.add(variantDocument);
        }
        return returnDocuments;
    }

    public void mapAttributes(VariantContext ctx, VariantDocument variantDocument) {
        CommonInfo info = ctx.getCommonInfo();
        List<String> significance = new ArrayList<>();
        List<String> evidences = new ArrayList<>();
        
        // 20% CPU
//      for(String key: ClinicalSig.csmap.keySet()) {
//          if(info.hasAttribute(key)) {
//              significance.add(ClinicalSig.csmap.get(key));
//          }
//      }
//      for(String key: Evidence.emap.keySet()) {
//          if(info.hasAttribute(key)) {
//              evidences.add(Evidence.emap.get(key));
//          }
//      }
        
        // 7% CPU
        for (String key : info.getAttributes().keySet()) {
            if (Evidence.emap.containsKey(key)) {
                evidences.add(Evidence.emap.get(key));
            } else if (ClinicalSig.csmap.containsKey(key)) {
                significance.add(ClinicalSig.csmap.get(key));
            }
        }
        variantDocument.setEvidence(evidences);
        variantDocument.setClinicalSignificance(significance);
    }

    public List<TranscriptFeature> getConsequences(VariantContext ctx, String varNuc, String[] header) throws Exception {
        List<TranscriptFeature> features = new ArrayList<>();

        String reference = ctx.getReference().toString();
        
        for (String s : ctx.getAttributeAsStringList("CSQ", "")) {
            if (s.length() > 0) {
                String[] infos = s.split("\\|", -1);

                if (header.length == infos.length) {
                    if (infos[0].equalsIgnoreCase(varNuc)) {
                        TranscriptFeature feature = new TranscriptFeature(header, infos);
                        feature.setReferenceSequence(reference);
                        features.add(feature);
                    }
                } else {
                    String message = "Diff: " + header.length + " " + infos.length;
                    message += "\r" + Join.join("|", header);
                    message += "\r" + String.join("|", Arrays.asList(infos));
                    throw new RuntimeException("CSQ header is not matching the line " + message);
                }
            }
        }
        return features;
    }


    public boolean alleleIsValid(String allele) {
        int len = allele.length();
        for (int i = 0; i < len; i++) {
            char c = allele.charAt(i);
            if (c == 'A' || c == 'C' || c == 'G' || c == 'T' || c == 'N' || c == '-')
                continue;
            return false;
        }
        return true;
    }


}
