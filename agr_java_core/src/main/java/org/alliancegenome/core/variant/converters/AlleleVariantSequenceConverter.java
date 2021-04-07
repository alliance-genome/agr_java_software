package org.alliancegenome.core.variant.converters;

import java.util.*;

import org.alliancegenome.api.entity.AlleleVariantSequence;
import org.alliancegenome.neo4j.entity.SpeciesType;
import org.alliancegenome.neo4j.entity.node.*;

import htsjdk.variant.variantcontext.VariantContext;
import io.github.lukehutch.fastclasspathscanner.utils.Join;

public class AlleleVariantSequenceConverter {
    
    public List<AlleleVariantSequence> convertContextToAlleleVariantSequence(VariantContext ctx, String[] header, SpeciesType speciesType) throws Exception {
        List<AlleleVariantSequence> returnDocuments = new ArrayList<AlleleVariantSequence>();

        htsjdk.variant.variantcontext.Allele refNuc = ctx.getReference();
        Species species = new Species();
        species.setName(speciesType.getName());
        species.setCommonNames(speciesType.getDisplayName());
        species.setId(Long.valueOf(speciesType.getTaxonIDPart()));
        species.setPrimaryKey(speciesType.getTaxonID());

        SOTerm variantType = new SOTerm();
        variantType.setName(ctx.getType().name().toUpperCase());
        variantType.setPrimaryKey(ctx.getType().name());
        if ("INDEL".equals(ctx.getType().name())) {
            variantType.setName("delins");
            variantType.setPrimaryKey("delins");
        }

        for (htsjdk.variant.variantcontext.Allele a : ctx.getAlternateAlleles()) {
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
            List<String> transcriptsProcessed=new ArrayList<>();

            AlleleVariantSequence s = new AlleleVariantSequence();

            Variant variant = new Variant();
            variant.setVariantType(variantType);
            variant.setSpecies(species);
            variant.setStart(String.valueOf(ctx.getStart()));
            variant.setEnd(String.valueOf(endPos));
            variant.setNucleotideChange(a.getBaseString()); // variantDocument.setVarNuc(a.getBaseString());
            boolean first=true;
            Set<String> molecularConsequences = new HashSet<>();
            Set<String> genes = new HashSet<>();
            List<TranscriptLevelConsequence> htpConsequences = getConsequences(ctx, a.getBaseString(), header);
            String hgvsNomenclature = htpConsequences != null ? htpConsequences.stream()
                    .findFirst()
                    .map(TranscriptLevelConsequence::getHgvsVEPGeneNomenclature)
                    .orElse(null) : null;

            variant.setHgvsNomenclature(hgvsNomenclature);
            variant.setName(hgvsNomenclature);
            if(ctx.getID()!=null && !ctx.getID().equals("") && !ctx.getID().equals(".")){
                s.setPrimaryKey(ctx.getID());
                s.setId(ctx.getID());
                s.setNameKey(ctx.getID());
                s.setName(ctx.getID());

            }else{
                //    if (hgvsNomenclature != null && hgvsNomenclature.length()<512) {
                if (hgvsNomenclature != null && hgvsNomenclature.length()<100) {

                    s.setPrimaryKey(hgvsNomenclature);
                    s.setId(hgvsNomenclature);
                    s.setNameKey(hgvsNomenclature);
                    s.setName(hgvsNomenclature);
                }

            }


            //    System.out.println("CONTEXT ID: "+ ctx.getID());
            s.setVariant(variant);
            if (htpConsequences != null) {
                for (TranscriptLevelConsequence c : htpConsequences) {
                    if(!transcriptsProcessed.contains(c.getTranscriptID())) {
                        transcriptsProcessed.add(c.getTranscriptID());
                        if(first) {
                            first=false;
                            //    variant.setHgvsNomenclature(c.getHgvsVEPGeneNomenclature());
                            variant.setGene(c.getAssociatedGene());

                        }
                        molecularConsequences.add(c.getTranscriptLevelConsequence());
                        //    s.setConsequence(c);
                        /****************SearchbleDocument Fields***************/
                        if(c.getAssociatedGene().getSymbol()!=null && !c.getAssociatedGene().getSymbol().equals(""))
                            genes.add(c.getAssociatedGene().getSymbol());

                    }
                }
            }
            s.setAlterationType("variant");
            s.setCategory("allele");
            s.setMolecularConsequence(molecularConsequences);
            s.setGenes(genes);
            s.setSpecies(species.getName());
            s.setChromosomes(Collections.singleton(ctx.getContig()));
            s.setChromosome(ctx.getContig());
            s.setTranscriptLevelConsequences(htpConsequences);
            s.setVariantType(Collections.singleton(variantType.getName()));
            returnDocuments.add(s);
        }
        return returnDocuments;
    }

    
    
    private List<TranscriptLevelConsequence> getConsequences(VariantContext ctx, String varNuc, String[] header) throws Exception {
        List<TranscriptLevelConsequence> features = new ArrayList<>();
        List<String> alreadyAdded = new ArrayList<>();
        
        for (String s : ctx.getAttributeAsStringList("CSQ", "")) {
            if (s.length() > 0) {
                String[] infos = s.split("\\|", -1);

                if (header.length == infos.length) {
                    if (infos[0].equalsIgnoreCase(varNuc)) {
                        TranscriptLevelConsequence feature = new TranscriptLevelConsequence(header, infos);
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

    private boolean alleleIsValid(String allele) {
        for (int i = 0; i < allele.length(); i++) {
            char c = allele.charAt(i);
            if (c == 'A' || c == 'C' || c == 'G' || c == 'T' || c == 'N' || c == '-')
                continue;
            return false;
        }
        return true;
    }

}
