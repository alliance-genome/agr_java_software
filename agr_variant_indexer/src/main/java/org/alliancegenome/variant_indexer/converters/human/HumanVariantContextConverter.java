package org.alliancegenome.variant_indexer.converters.human;

import java.util.*;
import java.util.stream.Collectors;

import org.alliancegenome.neo4j.entity.SpeciesType;
import org.alliancegenome.variant_indexer.converters.VariantContextConverter;
import org.alliancegenome.variant_indexer.es.model.*;

import com.fasterxml.jackson.core.JsonProcessingException;

import htsjdk.variant.variantcontext.*;

public class HumanVariantContextConverter extends VariantContextConverter {

    private static HumanVariantContextConverterHelper utils = new HumanVariantContextConverterHelper();

    public List<String> convertVariantContext(VariantContext ctx, SpeciesType speciesType) {

        List<String> returnDocuments = new ArrayList<String>();
        
        Allele refNuc = ctx.getReference();

        int index = 0;
        for (Allele a : ctx.getAlternateAlleles()) {
            VariantDocument variantDocument = new VariantDocument();
            variantDocument.setCategory("allele");
            variantDocument.setAlterationType("variant");

            //todo: need to generate actual hgvs
            String hgvsNomenclature = ctx.getContig() + ':' + ctx.getStart() + "-not-real-hgvs";
            variantDocument.setName(hgvsNomenclature);
            variantDocument.setNameKey(hgvsNomenclature);

            //todo: need to translate these types to match what we use for alleles
            variantDocument.setVariantType(new HashSet<>() {{ add(ctx.getType().name()); }});
            variantDocument.setId(ctx.getID());
            variantDocument.setSpecies(speciesType.getName());
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
            variantDocument.setDocumentVariantType((String) ctx.getCommonInfo().getAttribute("TSA"));
            try {
                if(speciesType == SpeciesType.HUMAN)
                    variantDocument.setConsequences(utils.getHumanConsequences(ctx, index, a.getBaseString()));
                else
                    variantDocument.setConsequences(utils.getConsequences(ctx, index, a.getBaseString()));
                    
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }

            //todo: this will need an id->name map for genes
            variantDocument.setGenes(
                variantDocument.getConsequences()
                        .stream()
                        .map(x -> x.getGene() + " (" + speciesType.getAbbreviation() + ")")
                        .collect(Collectors.toSet())
            );

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
            if (ctx.getAttribute("FILTER") != null)
                variantDocument.setQual((String) ctx.getAttribute("FILTER"));
            index = index + 1;
            List<Genotype> genotypes=ctx.getGenotypes();
            if(genotypes!=null && genotypes.size()>0) {
                List<Sample> samples = new ArrayList<>();
                for (Genotype g : genotypes) {
                    Sample s = new Sample();
                    s.setSampleName(g.getSampleName());
                    s.setDepth(g.getDP());
                    s.setType(g.getType().name());
                    samples.add(s);
                }
                variantDocument.setSamples(samples);
            }
            try {
                returnDocuments.add(mapper.writeValueAsString(variantDocument));
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }
        
        return returnDocuments;

    }
}
