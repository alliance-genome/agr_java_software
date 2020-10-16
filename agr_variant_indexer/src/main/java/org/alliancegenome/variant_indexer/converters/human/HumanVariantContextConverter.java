package org.alliancegenome.variant_indexer.converters.human;

import java.util.*;
import java.util.stream.Collectors;

import org.alliancegenome.neo4j.entity.SpeciesType;
import org.alliancegenome.variant_indexer.converters.VariantContextConverter;
import org.alliancegenome.variant_indexer.es.model.*;

import com.fasterxml.jackson.core.JsonProcessingException;

import htsjdk.variant.variantcontext.*;
import io.github.lukehutch.fastclasspathscanner.utils.Join;

public class HumanVariantContextConverter extends VariantContextConverter {

    public List<VariantDocument> convertVariantContext(VariantContext ctx, SpeciesType speciesType, String[] header) {

        List<VariantDocument> returnDocuments = new ArrayList<VariantDocument>();

        Allele refNuc = ctx.getReference();

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

            int endPos = 0;

            if (ctx.isSNP()) {
                endPos = ctx.getStart() + 1;
            } // insertions
            if (ctx.isSimpleInsertion()) {
                endPos = ctx.getStart();
                //  System.out.println("INSERTION");
            } else if (ctx.isSimpleDeletion()) {
                endPos = ctx.getStart() + refNuc.getDisplayString().length();
                //  System.out.println("Deletion");
            } else {
                //   System.out.println("Unexpected var type");
            }


            variantDocument.setRefNuc(refNuc.getBaseString());
            variantDocument.setVarNuc(a.getBaseString());
            variantDocument.setEndPos(endPos);
            variantDocument.setDocumentVariantType(ctx.getCommonInfo().getAttributeAsString("TSA", ""));
            try {
                variantDocument.setConsequences(getConsequences(ctx, a.getBaseString(), header));   
            } catch (Exception e) {
                e.printStackTrace();
            }

            //todo: this will need an id->name map for genes
            variantDocument.setGenes(
                    variantDocument.getConsequences()
                    .stream()
                    .map(x -> x.get("Gene") + " (" + speciesType.getAbbreviation() + ")")
                    .collect(Collectors.toSet())
                    );

            variantDocument.setEvidence(mapEvidence(ctx));
            variantDocument.setClinicalSignificance(mapClinicalSignificance(ctx));
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

            List<Genotype> genotypes=ctx.getGenotypes();
            
            if(genotypes != null && genotypes.size() > 0) {
                List<String> samples = new ArrayList<>();
                for (Genotype g : genotypes) {
                    if(!g.getType().name().equals("NO_CALL")) {
                        samples.add(g.getSampleName());
                    }
                }
                variantDocument.setSamples(samples);
            }

            returnDocuments.add(variantDocument);
        }

        return returnDocuments;

    }

    public List<String> mapEvidence(VariantContext ctx){
        CommonInfo info = ctx.getCommonInfo();
        List<String> evidences = new ArrayList<>();
        String key;
        String value;
        for (Map.Entry e: Evidence.emap.entrySet()) {
            key = (String) e.getKey();
            value = (String) e.getValue();
            if(info.getAttributes().containsKey(key)) {
                if (info.getAttributeAsBoolean(key, false)) {
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
        for (Map.Entry e: ClinicalSig.csmap.entrySet() ) {
            key = (String) e.getKey();
            value = (String) e.getValue();
            if(info.getAttributes().containsKey(key)) {
                if (info.getAttributeAsBoolean(key, false)) {
                    significance.add(value);
                }
            }
        }
        return significance;
    }


    public List<Map<String, String>> getConsequences(VariantContext ctx, String varNuc, String[] header) throws Exception {
        List<Map<String, String>> features = new ArrayList<>();

        for(String s: ctx.getAttributeAsStringList("CSQ", "")) {
            if(s.length() > 0) {
                String[] infos = s.split("\\|", -1);

                if(header.length == infos.length) {
                    HashMap<String, String> feature = new HashMap<String, String>();
                    for(int i = 0; i < header.length; i++) {
                        feature.put(header[i], infos[i]);
                    }
                    if(feature.get("Allele").equalsIgnoreCase(varNuc)) {
                        features.add(feature);
                    }
                } else {
                    System.out.println("Diff: " + header.length + " " + infos.length);
                    System.out.println(Join.join("|", header));
                    System.out.println(s);
                    for(int i = 0; i < infos.length; i++) {
                        System.out.println("Value: " + infos[i]);
                    }
                }
            }
        }
        return features;
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
