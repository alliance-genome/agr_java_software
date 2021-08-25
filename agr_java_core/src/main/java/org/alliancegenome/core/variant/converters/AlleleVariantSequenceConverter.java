package org.alliancegenome.core.variant.converters;

import htsjdk.variant.variantcontext.VariantContext;
import io.github.lukehutch.fastclasspathscanner.utils.Join;
import org.alliancegenome.api.entity.AlleleVariantSequence;
import org.alliancegenome.es.index.site.cache.GeneDocumentCache;
import org.alliancegenome.neo4j.entity.SpeciesType;
import org.alliancegenome.neo4j.entity.node.*;
import org.alliancegenome.neo4j.entity.relationship.GenomeLocation;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class AlleleVariantSequenceConverter {

    private static Pattern validAlleles = Pattern.compile("[ACGTN\\-]+");
    private Species species = null;

    public List<AlleleVariantSequence> convertContextToAlleleVariantSequence(VariantContext ctx, String[] header, SpeciesType speciesType, GeneDocumentCache geneCache) throws Exception {
        List<AlleleVariantSequence> returnDocuments = new ArrayList<>();

        //htsjdk.variant.variantcontext.Allele refNuc = ctx.getReference();

        if (species == null) {
            species = new Species();
            species.setName(speciesType.getName());
            species.setCommonNames(speciesType.getDisplayName());
            species.setPrimaryKey(speciesType.getTaxonID());
        }

        SOTerm variantType = new SOTerm();

        if (!"SYMBOLIC".equals(ctx.getType().name()) && !"MIXED".equals(ctx.getType().name())) {
            variantType.setName(ctx.getType().name().toUpperCase());
            variantType.setPrimaryKey(ctx.getType().name());
            if ("INDEL".equals(ctx.getType().name())) {
                variantType.setName("delins");
                variantType.setPrimaryKey("delins");
            }
        }


        GenomeLocation location = new GenomeLocation();
        location.setStart((long) ctx.getStart());
        location.setEnd((long) ctx.getEnd());
        Chromosome chromosome = new Chromosome();
        chromosome.setPrimaryKey(ctx.getContig());
        location.setChromosome(chromosome);

        for (htsjdk.variant.variantcontext.Allele vcfAllele : ctx.getAlternateAlleles()) {
            Allele agrAllele = new Allele(null, GeneticEntity.CrossReferenceType.VARIANT);

            Variant variant = new Variant();
            variant.setVariantType(variantType);
            variant.setLocation(location);

//          These cases do not exist in the human file or the mod files
//          if (vcfAllele.compareTo(refNuc) < 0) {
//              System.out.println("does this ever happen: " + vcfAllele);
//              continue;
//          }
//          if (!alleleIsValid(ctx.getReference().getBaseString())) {
//              System.out.println(" *** 1. Ref Nucleotides must be A,C,G,T,N: " + ctx.getReference().getBaseString());
//              continue;
//          }

            if (!alleleIsValid(vcfAllele.getBaseString())) {
                //System.out.println(" *** 2. Var Nucleotides must be A,C,G,T,N: " + vcfAllele.getBaseString());
                continue;
            }

            List<TranscriptLevelConsequence> htpConsequences = getConsequences(ctx, vcfAllele.getBaseString(), header, geneCache, species);
            if (htpConsequences.size() == 0) {
                continue;
            }

            String hgvsNomenclature = htpConsequences.stream()
                    .findFirst()
                    .map(TranscriptLevelConsequence::getHgvsVEPGeneNomenclature)
                    .orElse(null);

            variant.setGenomicReferenceSequence(ctx.getReference().getBaseString());
            variant.setGenomicVariantSequence(vcfAllele.getBaseString());

            HashSet<String> transcriptsProcessed = new HashSet<>();

            AlleleVariantSequence avsDoc = new AlleleVariantSequence();

            variant.setNucleotideChange(vcfAllele.getBaseString()); // variantDocument.setVarNuc(a.getBaseString());
            Set<String> molecularConsequences = new HashSet<>();
            Set<String> geneLevelConsequences = new HashSet<>();
            Set<String> genes = new HashSet<>();
            Set<String> geneIds = new HashSet<>();


            StringBuilder variantName = new StringBuilder();
            if (StringUtils.isNotEmpty(hgvsNomenclature)) {
                variantName.append('(')
                        .append(speciesType.getAssembly())
                        .append(')')
                        .append(chromosome.getPrimaryKey())
                        .append(':')
                        .append(hgvsNomenclature.split(":")[1].substring(2));
            }
            variant.setName(variantName.toString());
            variant.setHgvsNomenclature(hgvsNomenclature);
            avsDoc.setVariantName(variant.getName());

            String ctxId = ctx.getID();
            if (StringUtils.isNotEmpty(ctxId) && !ctxId.equals(".")) {
                avsDoc.setPrimaryKey(ctxId);
                avsDoc.setNameKey(ctxId);
                avsDoc.setName(ctxId);
                variant.setPrimaryKey(ctxId);

            } else {
                //    if (hgvsNomenclature != null && hgvsNomenclature.length()<512) {
                if (hgvsNomenclature != null && hgvsNomenclature.length() < 100) {
                    variant.setPrimaryKey(hgvsNomenclature);
                    avsDoc.setPrimaryKey(hgvsNomenclature);
                    avsDoc.setNameKey(hgvsNomenclature);
                    avsDoc.setName(hgvsNomenclature);
                }

            }
            agrAllele.setPrimaryKey(variant.getPrimaryKey());
            agrAllele.setSymbol(hgvsNomenclature);
            agrAllele.setSymbolText(hgvsNomenclature);
            Set<String> geneSynonymSet = new HashSet<>();
            Set<String> geneCrossReferencesSet = new HashSet<>();

            boolean firstTranscript = true;

            if (htpConsequences != null) {
                for (TranscriptLevelConsequence consequence : htpConsequences) {
                    Gene consequenceGene = consequence.getAssociatedGene();

                    String transcriptID = consequence.getTranscript().getPrimaryKey();

                    if (!transcriptsProcessed.contains(transcriptID)) {
                        transcriptsProcessed.add(transcriptID);

                        if (firstTranscript) {
                            if (consequenceGene != null) {
                                if (variant.getGene() == null) {
                                    variant.setGene(consequenceGene);
                                }
                                if (geneCache != null) {
                                    geneSynonymSet = geneCache.getSynonyms().get(consequenceGene.getPrimaryKey());
                                    geneCrossReferencesSet = geneCache.getCrossReferences().get(consequenceGene.getPrimaryKey());
                                }
                                firstTranscript = false;
                            }
                        }

                        molecularConsequences.addAll(consequence.getMolecularConsequences());
                        geneLevelConsequences.add(consequence.getGeneLevelConsequence());
                        if (consequenceGene != null && StringUtils.isNotEmpty(consequenceGene.getSymbol())) {
                            // This is faster than calling getNakeKey on the gene
                            StringBuffer buffer = new StringBuffer();
                            buffer.append(consequenceGene.getSymbol());
                            buffer.append(" (");
                            buffer.append(speciesType.getAbbreviation());
                            buffer.append(")");
                            genes.add(buffer.toString());
                            geneIds.add(consequenceGene.getPrimaryKey());

                        }
                    }
                }
            }
            avsDoc.setGeneSynonyms(geneSynonymSet);
            avsDoc.setGeneCrossReferences(geneCrossReferencesSet);
            variant.setTranscriptLevelConsequence(htpConsequences);
            variant.setSpecies(species);
            GeneLevelConsequence geneLevelConsequence = new GeneLevelConsequence();
            geneLevelConsequence.setGeneLevelConsequence(String.join(",", geneLevelConsequences));
            variant.setGeneLevelConsequence(geneLevelConsequence);
            agrAllele.setVariants(Arrays.asList(variant));
            avsDoc.setAlterationType("variant");
            avsDoc.setCategory("allele");
            avsDoc.setMolecularConsequence(molecularConsequences);
            avsDoc.setGenes(genes);
            avsDoc.setGeneIds(geneIds);
            avsDoc.setSpecies(species.getName());
            avsDoc.setChromosome(ctx.getContig());
            avsDoc.setVariantType(Collections.singleton(variantType.getName()));
            avsDoc.setAllele(agrAllele);
            returnDocuments.add(avsDoc);
        }


        return returnDocuments;
    }

    private List<TranscriptLevelConsequence> getConsequences(VariantContext ctx, String varNuc, String[] header, GeneDocumentCache geneCache, Species species) throws Exception {
        List<TranscriptLevelConsequence> features = new ArrayList<>();
        HashSet<String> alreadyAdded = new HashSet<>();

        for (String s : ctx.getAttributeAsStringList("CSQ", "")) {
            if (s.length() > 0) {
                String[] infos = s.split("\\|", -1);

                if (header.length == infos.length) {
                    if (infos[0].equalsIgnoreCase(varNuc)) {

                        TranscriptLevelConsequence feature = new TranscriptLevelConsequence(header, infos, geneCache, species);

                        Transcript transcript = feature.getTranscript();
                        
                        if (transcript != null) {
                            String transcriptID = transcript.getPrimaryKey();
                            if (!alreadyAdded.contains(transcriptID)) {
                                features.add(feature);
                                alreadyAdded.add(transcriptID);
                            }
                        }
                    }
                } else {
                    String message = "Diff: " + header.length + " " + infos.length;
                    message += "\r" + Join.join("|", header);
                    message += "\r" + String.join("|", Arrays.asList(infos));
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
