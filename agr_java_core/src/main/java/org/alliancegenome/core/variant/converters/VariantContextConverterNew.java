package org.alliancegenome.core.variant.converters;

import htsjdk.variant.variantcontext.Allele;
import htsjdk.variant.variantcontext.CommonInfo;
import htsjdk.variant.variantcontext.Genotype;
import htsjdk.variant.variantcontext.VariantContext;
import io.github.lukehutch.fastclasspathscanner.utils.Join;
import org.alliancegenome.api.entity.AlleleVariantSequence;
import org.alliancegenome.es.variant.model.ClinicalSig;
import org.alliancegenome.es.variant.model.Evidence;
import org.alliancegenome.neo4j.entity.SpeciesType;
import org.alliancegenome.neo4j.entity.node.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class VariantContextConverterNew {
    public List<AlleleVariantSequence> convertVariantContext(VariantContext ctx, SpeciesType speciesType, String[] header) throws Exception {

        List<AlleleVariantSequence> returnDocuments = new ArrayList<AlleleVariantSequence>();

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
            Species species=new Species();
            species.setName(speciesType.getName());
            species.setCommonNames(speciesType.getDisplayName());
            species.setId(Long.valueOf(speciesType.getTaxonIDPart()));
            species.setPrimaryKey(speciesType.getTaxonID());
            SOTerm variantType=new SOTerm();
            variantType.setName(ctx.getType().name().toUpperCase());
            variantType.setPrimaryKey(ctx.getType().name());
            if ("INDEL".equals(ctx.getType().name())) {
                variantType.setName( "delins");
                variantType.setPrimaryKey("delins");
            }
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
           for(TranscriptLevelConsequence c: getConsequences(ctx, a.getBaseString(), header)){
               AlleleVariantSequence s=new AlleleVariantSequence();
               org.alliancegenome.neo4j.entity.node.Allele allele =new org.alliancegenome.neo4j.entity.node.Allele();
               Variant variant=new Variant();
               variant.setVariantType(variantType);

               variant.setSpecies(species);
               variant.setStart(String.valueOf(ctx.getStart()));
               variant.setEnd(String.valueOf(endPos));
               variant.setNucleotideChange(a.getBaseString()); // variantDocument.setVarNuc(a.getBaseString());
               variant.setName(c.getHgvsVEPGeneNomenclature());
                s.setVariant(variant);
                s.setConsequence(c);
               returnDocuments.add(s);
           }






       /*     THIS BELOW FIELDS ARE NOT AVAILABLE FOR MOD HTP VARIANTS, but available for HUMAN
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
    */

            List<Genotype> genotypes = ctx.getGenotypes();
/*
            if(genotypes != null && genotypes.size() > 0) {
                List<String> samples = new ArrayList<>();
                for (Genotype g : genotypes) {
                    if(!g.getType().name().equals("NO_CALL")) {
                        samples.add(g.getSampleName());
                    }
                }
                variantDocument.setSamples(samples);
            }
*/


        }

        return returnDocuments;

    }

    public List<String> mapEvidence(VariantContext ctx) {
        CommonInfo info = ctx.getCommonInfo();
        List<String> evidences = new ArrayList<>();
        for (String key : info.getAttributes().keySet()) {
            if (Evidence.emap.containsKey(key)) {
                evidences.add(Evidence.emap.get(key));
            }
        }
        return evidences;
    }

    public List<String> mapClinicalSignificance(VariantContext ctx) {
        CommonInfo info = ctx.getCommonInfo();
        List<String> significance = new ArrayList<>();
        for (String key : info.getAttributes().keySet()) {

            if (ClinicalSig.csmap.containsKey(key)) {
                significance.add(ClinicalSig.csmap.get(key));
            }
        }
        return significance;
    }


    public List<TranscriptLevelConsequence> getConsequences(VariantContext ctx, String varNuc, String[] header) throws Exception {
        List<TranscriptLevelConsequence> features = new ArrayList<>();
        List<String> alreadyAdded=new ArrayList<>();
        for (String s : ctx.getAttributeAsStringList("CSQ", "")) {
            if (s.length() > 0) {
                String[] infos = s.split("\\|", -1);

                if (header.length == infos.length) {
                    if (infos[0].equalsIgnoreCase(varNuc)) {
                        TranscriptLevelConsequence feature = getTranscriptLevelConsq(infos);
                        if(!alreadyAdded.contains(feature.getTranscriptID())) {
                            features.add(feature);
                            alreadyAdded.add(feature.getTranscriptID());
                        }
                    }
                } else {
                    String message = "Diff: " + header.length + " " + infos.length;
                    message += "\r" + Join.join("|", header);
                    message += "\r" +String.join("|", Arrays.asList(infos));
                    throw new RuntimeException("CSQ header is not matching the line " + message);
                }
            }
        }
        return features;
    }
    public TranscriptLevelConsequence  getTranscriptLevelConsq(String[] infos ){
        TranscriptLevelConsequence c=new TranscriptLevelConsequence();
        c.setTranscriptLevelConsequence( infos[1]);
        c.setImpact( infos[2]);
        Gene g=new Gene();
        g.setSymbol(infos[3]);
        g.setPrimaryKey(infos[4]);
        c.setAssociatedGene(g);
        c.setSequenceFeatureType( infos[7]); ///need to verify
        c.setTranscriptID(infos[6]);
        if(!infos[8].equals("")) // need to look in the VCF file
        c.setTranscriptLocation( infos[8]);
        if(!infos[9].equals("")) // need to look in the VCF file
            c.setTranscriptLocation( infos[9]);

      /*  biotype = infos[7];
        exon = infos[8];
        intron = infos[9];*/
        c.setCdnaStartPosition(infos[12]);
        c.setCdsStartPosition( infos[13]);
        c.setHgvsCodingNomenclature( infos[10]);
        c.setHgvsProteinNomenclature(infos[11]);
        c.setProteinStartPosition(infos[14]);
        c.setAminoAcidChange(infos[15]);
        c.setCodonChange(infos[16]);
        c.setPolyphenPrediction(infos[30]);
        c.setSiftPrediction(infos[29]);
        c.setHgvsVEPGeneNomenclature(infos[32]);
        c.setCodonReference(infos[26]); // need to verify


      /*  existingVariation = infos[17];
        distance = infos[18];
        strand = infos[19];

        flags = infos[20];
        symbolSource = infos[21];
        hgncId = infos[22];
        refseqMatch = infos[23];
        source = infos[24];
        refseqOffset = infos[25];
        givenRef = infos[26];
        usedRef = infos[27];
        bamEdit = infos[28];

        hgvsOffset = infos[31];*/
        return c;
    }

    public boolean alleleIsValid(String allele) {
        for (int i = 0; i < allele.length(); i++) {
            char c = allele.charAt(i);
            if (c == 'A' || c == 'C' || c == 'G' || c == 'T' || c == 'N' || c == '-')
                continue;
            return false;
        }
        return true;
    }


}
