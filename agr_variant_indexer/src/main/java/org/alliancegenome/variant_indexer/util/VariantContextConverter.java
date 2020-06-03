package org.alliancegenome.variant_indexer.util;

import java.util.ArrayList;
import java.util.List;

import org.alliancegenome.variant_indexer.model.VariantDocument;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import htsjdk.variant.variantcontext.Allele;
import htsjdk.variant.variantcontext.VariantContext;

public class VariantContextConverter {

    private static VCFUtils utils = new VCFUtils();
    private static ObjectMapper mapper = new ObjectMapper();

    public static List<String> convertVariantContext(VariantContext ctx) {

        List<String> returnDocuments = new ArrayList<String>();
        
        Allele refNuc = ctx.getReference();

        int index = 0;
        for (Allele a : ctx.getAlternateAlleles()) {
            VariantDocument variantDocument = new VariantDocument();
            variantDocument.setId(ctx.getID());
            variantDocument.setChromosome(ctx.getContig());
            variantDocument.setStartPos(ctx.getStart());
            if (a.compareTo(refNuc) < 0) {
                continue;
            }
            int endPos = 0;

            if (ctx.isSNP()) {
                endPos = ctx.getStart() + 1;
            } // insertions
            if (ctx.isSimpleInsertion()) {
                endPos = ctx.getStart();
                //  System.out.println("INSERTION");
            }
            // deletions
            else if (ctx.isSimpleDeletion()) {
                endPos = ctx.getStart() + refNuc.getDisplayString().length();
                //  System.out.println("Deletion");
            } else {
                //   System.out.println("Unexpected var type");
            }
            if (!utils.alleleIsValid(ctx.getReference().getBaseString())) {
                //   System.out.println(" *** Ref Nucleotides must be A,C,G,T,N");
                continue;
            }
            if (!utils.alleleIsValid(a.getBaseString())) {
                //     System.out.println(" *** Var Nucleotides must be A,C,G,T,N");
                continue;
            }

            variantDocument.setRefNuc(refNuc.getBaseString());
            variantDocument.setVarNuc(a.getBaseString());
            variantDocument.setEndPos(endPos);
            variantDocument.setVariantType((String) ctx.getCommonInfo().getAttribute("TSA"));
            try {
                variantDocument.setConsequences(utils.getConsequences(ctx, index, a.getBaseString()));
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
            variantDocument.setEvidence(utils.mapEvidence(ctx));
            variantDocument.setClinicalSignificance(utils.mapClinicalSignificance(ctx));
            if (ctx.getAttribute("MA") != null)
                variantDocument.setMA(ctx.getAttribute("MA").toString());
            if (ctx.getAttribute("MAF") != null)
                variantDocument.setMAF(Double.parseDouble(ctx.getAttribute("MAF").toString()));
            if (ctx.getAttribute("MAC") != null)
                variantDocument.setMAC(Integer.parseInt(ctx.getAttribute("MAC").toString()));
            if (ctx.getAttribute("RefPep") != null)
                variantDocument.setRefPep((String) ctx.getAttribute("RefPep"));
            if (ctx.getAttribute("AA") != null)
                variantDocument.setAa((String) ctx.getAttribute("AA"));
            if (ctx.getAttribute("QUAL") != null)
                variantDocument.setQual((String) ctx.getAttribute("QUAL"));
            
            index = index + 1;

            try {
                returnDocuments.add(mapper.writeValueAsString(variantDocument));
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }
        
        return returnDocuments;

    }

}
