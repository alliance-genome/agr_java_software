package org.alliancegenome.core.variant.converters;

import java.util.*;
import java.util.regex.*;

import org.alliancegenome.api.entity.AlleleVariantSequence;
import org.alliancegenome.es.index.site.cache.GeneDocumentCache;
import org.alliancegenome.neo4j.entity.SpeciesType;
import org.alliancegenome.neo4j.entity.node.*;
import org.alliancegenome.neo4j.entity.relationship.GenomeLocation;
import org.apache.commons.lang3.StringUtils;

import htsjdk.variant.variantcontext.VariantContext;
import io.github.lukehutch.fastclasspathscanner.utils.Join;

public class AlleleVariantSequenceConverter {
    
    private static Pattern validAlleles = Pattern.compile("[ACGTN\\-]+");

    public List<AlleleVariantSequence> convertContextToAlleleVariantSequence(VariantContext ctx, String[] header, SpeciesType speciesType, GeneDocumentCache geneCache) throws Exception {
        List<AlleleVariantSequence> returnDocuments = new ArrayList<>();

        htsjdk.variant.variantcontext.Allele refNuc = ctx.getReference();
        Allele agrAllele= new Allele();
        agrAllele.setCrossReferenceType(GeneticEntity.CrossReferenceType.VARIANT);
        Species species = new Species();
        species.setName(speciesType.getName());
        species.setCommonNames(speciesType.getDisplayName());
        species.setId(Long.valueOf(speciesType.getTaxonIDPart()));
        species.setPrimaryKey(speciesType.getTaxonID());
        agrAllele.setSpecies(species);

        SOTerm variantType = new SOTerm();
        variantType.setName(ctx.getType().name().toUpperCase());
        variantType.setPrimaryKey(ctx.getType().name());
        if ("INDEL".equals(ctx.getType().name())) {
            variantType.setName("delins");
            variantType.setPrimaryKey("delins");
        }

        Variant variant = new Variant();

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

            variant.setGenomicReferenceSequence(ctx.getReference().getBaseString());
            variant.setGenomicVariantSequence(a.getBaseString());

            List<String> transcriptsProcessed=new ArrayList<>();

            AlleleVariantSequence avsDoc = new AlleleVariantSequence();

            variant.setVariantType(variantType);
            variant.setSpecies(species);
            GenomeLocation location = new GenomeLocation();
            location.setStart((long) ctx.getStart());
            location.setEnd((long) ctx.getEnd());
            Chromosome chromosome = new Chromosome();
            chromosome.setPrimaryKey(ctx.getContig());
            location.setChromosome(chromosome);
            variant.setLocation(location);
            variant.setNucleotideChange(a.getBaseString()); // variantDocument.setVarNuc(a.getBaseString());
            boolean first = true;
            Set<String> molecularConsequences = new HashSet<>();
            Set<String> genes = new HashSet<>();
            Set<String> geneIds=new HashSet<>();
            List<TranscriptLevelConsequence> htpConsequences = getConsequences(ctx, a.getBaseString(), header);
            String hgvsNomenclature = htpConsequences != null ? htpConsequences.stream()
                    .findFirst()
                    .map(TranscriptLevelConsequence::getHgvsVEPGeneNomenclature)
                    .orElse(null) : null;

            StringBuilder hgvsSynonym = new StringBuilder();
            if(hgvsNomenclature != null){
                hgvsSynonym.append('(')
                        .append(speciesType.getAssembly())
                        .append(')')
                        .append(chromosome.getPrimaryKey())
                        .append(':')
                        .append(hgvsNomenclature.split(":")[1].substring(2));
            }

            variant.setHgvsNomenclature(hgvsNomenclature);
            variant.setName(hgvsSynonym.toString());
            String ctxId = ctx.getID();
            if(StringUtils.isNotEmpty(ctxId) && !ctxId.equals(".")) {
                avsDoc.setPrimaryKey(ctxId);
                avsDoc.setNameKey(ctxId);
                avsDoc.setName(ctxId);
                variant.setPrimaryKey(ctxId);

            } else{
                //    if (hgvsNomenclature != null && hgvsNomenclature.length()<512) {
                if (hgvsNomenclature != null && hgvsNomenclature.length() < 100) {
                    variant.setPrimaryKey(hgvsNomenclature);
                    avsDoc.setPrimaryKey(hgvsNomenclature);
                    avsDoc.setNameKey(hgvsNomenclature);
                    avsDoc.setName(hgvsNomenclature);
                }

            }

            agrAllele.setSymbol(hgvsNomenclature);
            agrAllele.setSymbolText(hgvsNomenclature);
            //    System.out.println("CONTEXT ID: "+ ctx.getID());
     //       avsDoc.setVariant(variant);
            if (htpConsequences != null) {
                for (TranscriptLevelConsequence c : htpConsequences) {
                    if(geneCache != null){
                        Set<String> synonymSet = geneCache.getSynonyms().get(c.getAssociatedGene().getPrimaryKey());
                        Set<String> crossReferencesSet = geneCache.getCrossReferences().get(c.getAssociatedGene().getPrimaryKey());
                        if(synonymSet != null) {
                            List<String> synonymList = new ArrayList<>(synonymSet);
                            c.getAssociatedGene().setSynonymList(synonymList);
                        }

                        if(crossReferencesSet != null) {
                            List<String> crossReferencesList = new ArrayList<>(crossReferencesSet);
                            c.getAssociatedGene().setCrossReferencesList(crossReferencesList);
                        }
                    }


                    c.getAssociatedGene().setSpecies(species);
                    String transcriptID = c.getTranscript().getPrimaryKey();
                    if(!transcriptsProcessed.contains(transcriptID)) {
                        transcriptsProcessed.add(transcriptID);
                        if(first) {
                            first=false;
                            //    variant.setHgvsNomenclature(c.getHgvsVEPGeneNomenclature());
                            //c.getAssociatedGene().setSpecies(species);
                            variant.setGene(c.getAssociatedGene());
                            agrAllele.setGene(c.getAssociatedGene());

                        }
                        molecularConsequences.addAll(c.getTranscriptLevelConsequences());
                        //    s.setConsequence(c);
                        /****************SearchableDocument Fields***************/
                        if(StringUtils.isNotEmpty(c.getAssociatedGene().getSymbol())) {
                            // This is faster than calling getNakeKey on the gene
                            StringBuffer buffer = new StringBuffer();
                            buffer.append(c.getAssociatedGene().getSymbol());
                            buffer.append(" (");
                            buffer.append(speciesType.getAbbreviation());
                            buffer.append(")");
                            genes.add(buffer.toString());
                            geneIds.add(c.getAssociatedGene().getPrimaryKey());

                        }
                    }
                }
            }
            variant.setTranscriptLevelConsequence(htpConsequences);
            agrAllele.setVariants(Arrays.asList( variant));
            agrAllele.setUrl("");
            agrAllele.setModCrossRefCompleteUrl("");
            avsDoc.setAlterationType("variant");
            avsDoc.setCategory("allele");
            avsDoc.setMolecularConsequence(molecularConsequences);
            avsDoc.setGenes(genes);
            avsDoc.setGeneIds(geneIds);
            avsDoc.setSpecies(species.getName());
            avsDoc.setChromosomes(Collections.singleton(ctx.getContig()));
            avsDoc.setChromosome(ctx.getContig());
        //    avsDoc.setTranscriptLevelConsequences(htpConsequences);
            avsDoc.setVariantType(Collections.singleton(variantType.getName()));
            avsDoc.setAllele(agrAllele);
            returnDocuments.add(avsDoc);
        }
        return returnDocuments;
    }

    
    
    private List<TranscriptLevelConsequence> getConsequences(VariantContext ctx, String varNuc, String[] header) throws Exception {
        List<TranscriptLevelConsequence> features = new ArrayList<>();
        HashSet<String> alreadyAdded = new HashSet<>();
        
        for (String s : ctx.getAttributeAsStringList("CSQ", "")) {
            if (s.length() > 0) {
                String[] infos = s.split("\\|", -1);

                if (header.length == infos.length) {
                    if (infos[0].equalsIgnoreCase(varNuc)) {

                        TranscriptLevelConsequence feature = new TranscriptLevelConsequence(header, infos);
                        String transcriptID = feature.getTranscript().getPrimaryKey();
                        if(!alreadyAdded.contains(transcriptID)) {
                            features.add(feature);
                            alreadyAdded.add(transcriptID);
                        }
                    }
                } else {
                    String message = "Diff: " + header.length + " " + infos.length;
                    message += "\r" + Join.join("|", header);
                    message += "\r" +String.join("|", Arrays.asList(infos));
                    // throw new RuntimeException("CSQ header is not matching the line " + message);
                    // This has got to fail ... there is something not right with the files.
                    // The code is mis matched with the files. Or the files are old files and need to be deleted / updated
                    System.exit(-1);
                }
            }
        }
        return features;
    }

    private boolean alleleIsValid(String allele) {
        return validAlleles.matcher(allele).matches();
    }

}
