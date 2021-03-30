package org.alliancegenome.core.variant.converters;

import htsjdk.variant.variantcontext.CommonInfo;
import htsjdk.variant.variantcontext.VariantContext;
import io.github.lukehutch.fastclasspathscanner.utils.Join;
import org.alliancegenome.api.entity.AlleleVariantSequence;
import org.alliancegenome.es.variant.model.ClinicalSig;
import org.alliancegenome.es.variant.model.Evidence;
import org.alliancegenome.es.variant.model.TranscriptFeature;
import org.alliancegenome.es.variant.model.VariantDocument;
import org.alliancegenome.neo4j.entity.SpeciesType;
import org.alliancegenome.neo4j.entity.node.*;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

public class VariantContextConverterNew {

    AlleleVariantSequenceConverter converter = new AlleleVariantSequenceConverter();

    public List<AlleleVariantSequence> convertVariantContext(VariantContext ctx, SpeciesType speciesType, String[] header, Map<String, List<Allele>> alleleMap, List<String> matched) throws Exception {
        List<AlleleVariantSequence> returnDocuments = new ArrayList<>();
        if(!speciesType.getTaxonID().equalsIgnoreCase("NCBITaxon:9606")) {
            returnDocuments = getVariantsIfMatchedWithLTP(ctx, alleleMap, header, matched);
            // List<AlleleVariantSequence> returnDocuments = new ArrayList<AlleleVariantSequence>(variantSequences);
        }
        if(returnDocuments.size()==0){
            //    returnDocuments.addAll(converter.convertContextToAlleleVariantSequence(ctx, header, speciesType));
            returnDocuments = converter.convertContextToSearchDocument(ctx, header, speciesType);
            return returnDocuments;

        }
        return null;
    }
    public List<AlleleVariantSequence> getVariantsIfMatchedWithLTP(VariantContext ctx, Map<String, List<Allele>> alleleMap, String[] header, List<String> matched) throws Exception {
        htsjdk.variant.variantcontext.Allele refNuc = ctx.getReference();
        List<AlleleVariantSequence> returnDocuments = new ArrayList<>();
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
            String hgvsNomenclature = getHgvsG(ctx, a.getBaseString(), header);
            if (alleleMap != null && !alleleMap.isEmpty() && alleleMap.containsKey(hgvsNomenclature.trim())) {
                //  System.out.print("\tMATCHED: " + hgvsNomenclature);
                if (!matched.contains(hgvsNomenclature))
                    matched.add(hgvsNomenclature);
                List<Allele> alleles = alleleMap.get(hgvsNomenclature);
                for (Allele al : alleles) {
                    returnDocuments.addAll(converter.translateToNewAlleleVariantSequence(al, hgvsNomenclature, "true"));
                }
            }
        }
        return returnDocuments;
    }
    public List<VariantDocument> getVarDocs(List<Allele> alleles, String key, String species, String chromosome){
        List<VariantDocument> varDocs=new ArrayList<>();
        for (Allele a : alleles) {
            List<Variant> variants = a.getVariants();
            for (Variant v : variants) {
                VariantDocument doc = new VariantDocument();
                //  doc.setAllelesNew(alleles);
                Set<String> alleleset = new HashSet<>();
                alleleset.add(a.getGlobalId());
                doc.setAlleles(alleleset);
                doc.setName(key);
                doc.setNameKey(key);
                doc.setCategory("allele");
                doc.setAlterationType("variant");
                doc.setSpecies(species);
                doc.setChromosome(chromosome);
                if (v.getStart() != null)
                    doc.setStartPos(Integer.parseInt(v.getStart()));
                if (v.getEnd() != null)
                    doc.setEndPos(Integer.parseInt(v.getEnd()));
                Set<String> variantTypes = new HashSet<>();
                variantTypes.add(v.getVariantType().getName());
                doc.setVariantType(variantTypes);
                doc.setVarNuc(v.getNucleotideChange());
                Set<String> alleleIds = new HashSet<>();
                alleleIds.add(a.getGlobalId());
                alleleIds.add(a.getLocalId());
                if (a.getId() != null)
                    alleleIds.add(String.valueOf(a.getId()));
                doc.setAlleles(alleleIds);
                if (v.getTranscriptLevelConsequence() != null) {
                    List<TranscriptFeature> features = new ArrayList<>();

                    List<TranscriptLevelConsequence> con = v.getTranscriptLevelConsequence();
                    for (TranscriptLevelConsequence c : con) {
                        TranscriptFeature f = new TranscriptFeature();
                        f.setCdnaPosition(c.getCdnaStartPosition());
                        f.setCdsPosition(c.getCdsStartPosition());
                        f.setConsequence(c.getTranscriptLevelConsequence());
                        f.setFeatureType(c.getSequenceFeatureType());
                        if (c.getAssociatedGene() != null)
                            f.setGene(c.getAssociatedGene().getPrimaryKey());
                        f.setGivenRef(c.getAminoAcidReference());
                        features.add(f);
                    }
                    doc.setConsequences(features);
                }
                varDocs.add(doc);

            }
        }
        return varDocs;
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
    public List<AlleleVariantSequence> convertToAlleleVariantSequence(List<VariantDocument> varDocs){
        List<AlleleVariantSequence> list = new ArrayList<>();

        for (VariantDocument doc : varDocs) {
            List<AlleleVariantSequence> list1 = new ArrayList<>();
            if(doc.getConsequences()!=null && doc.getConsequences().size()>0){
                for (TranscriptFeature transcriptFeature : doc.getConsequences()) {
                    AlleleVariantSequence s=new AlleleVariantSequence();
                    s.setName(doc.getName());
                    s.setCategory("allele");
                    s.setAlleles(doc.getAlleles());
                    String geneID = transcriptFeature.getGene();
                    // do not handle variants without gene relationship
                    if (StringUtils.isEmpty(geneID))
                        continue;
                    Allele allele = new Allele(transcriptFeature.getGene(), GeneticEntity.CrossReferenceType.VARIANT);
                    // hack until the ID column is set to the right thing by the MODs
                    if (StringUtils.isEmpty(doc.getId()) || doc.getId().equals(".")) {
                        allele.setSymbol(transcriptFeature.getHgvsg());
                        allele.setSymbolText(transcriptFeature.getHgvsg());
                    } else {
                        allele.setSymbol(doc.getId());
                        allele.setSymbolText(doc.getId());
                    }
                    Gene gene = new Gene();
                    String assocatedGeneID = transcriptFeature.getGene();
                    /*if (assocatedGeneID.startsWith("ZDB-GENE"))
                        assocatedGeneID = "ZFIN:" + assocatedGeneID;*/
                    gene.setPrimaryKey(assocatedGeneID);
                    gene.setSymbol(transcriptFeature.getSymbol());
                    allele.setGene(gene);
                    Variant variant = new Variant();
                    TranscriptLevelConsequence consequence = new TranscriptLevelConsequence();
                    variant.setHgvsNomenclature(transcriptFeature.getHgvsc());
                    // TODO: Needs to be set somewhere does not come through the vcf file.
                    variant.setGenomicReferenceSequence(transcriptFeature.getReferenceSequence());
                    variant.setGenomicVariantSequence(transcriptFeature.getAllele());
                    variant.setStart(transcriptFeature.getGenomicStart());
                    variant.setEnd(transcriptFeature.getGenomicEnd());
                    variant.setConsequence((transcriptFeature.getConsequence()));
                    variant.setHgvsNomenclature(transcriptFeature.getHgvsg());
                    SOTerm soTerm = new SOTerm();
                    soTerm.setName(doc.getVariantType().stream().findFirst().get());
                    soTerm.setPrimaryKey(doc.getVariantType().stream().findFirst().get());
                    variant.setVariantType(soTerm);
                    consequence.setImpact(transcriptFeature.getImpact());
                    consequence.setSequenceFeatureType(transcriptFeature.getBiotype());
                    consequence.setTranscriptName(transcriptFeature.getFeature());
                    consequence.setTranscriptLevelConsequence(transcriptFeature.getConsequence());
                    consequence.setPolyphenPrediction(transcriptFeature.getPolyphenPrediction());
                    consequence.setPolyphenScore(transcriptFeature.getPolyphenScore());
                    consequence.setSiftPrediction(transcriptFeature.getSiftPrediction());
                    consequence.setSiftScore(transcriptFeature.getSiftScore());
                    consequence.setTranscriptLocation(transcriptFeature.getExon());
                    consequence.setAssociatedGene(gene);
                    String location = "";
                    if (StringUtils.isNotEmpty(transcriptFeature.getExon()))
                        location += "Exon " + transcriptFeature.getExon();
                    if (StringUtils.isNotEmpty(transcriptFeature.getIntron()))
                        location += "Intron " + transcriptFeature.getIntron();
                    consequence.setTranscriptLocation(location);
                    // list.add(new AlleleVariantSequence(allele, variant, consequence));
                    s.setVariant(variant);
                    s.setConsequence(consequence);
                    s.setAllele(allele);
                    s.setMatchedWithHtp("true");
                    list1.add(s);
                }
            }
            if(list1.size()==0) {
                AlleleVariantSequence s = new AlleleVariantSequence();
                s.setName(doc.getName());
                s.setCategory("allele");
                s.setAlleles(doc.getAlleles());
                s.setMatchedWithHtp("true");
                list.add(s);
            }else {
                list.addAll(list1);
            }
            AlleleVariantSequence s = new AlleleVariantSequence();
            s.setName(doc.getName());
            s.setCategory("allele");
            s.setAlleles(doc.getAlleles());
            s.setMatchedWithHtp("true");
            list.add(s);
        }
        System.out.println("LSIT SIZE IN METHOD: "+ list.size());
        return list;
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
    public String getHgvsG(VariantContext ctx, String varNuc, String[] header) throws Exception {
        List<TranscriptFeature> features = new ArrayList<>();

        for (String s : ctx.getAttributeAsStringList("CSQ", "")) {
            if (s.length() > 0) {
                String[] infos = s.split("\\|", -1);

                if (header.length == infos.length) {
                    if (infos[0].equalsIgnoreCase(varNuc)) {
                        TranscriptFeature feature = new TranscriptFeature(header, infos);
                        feature.setReferenceSequence(ctx.getReference().toString());
                        features.add(feature);
                    }
                } else {
                    String message = "Diff: " + header.length + " " + infos.length;
                    message += "\r" + Join.join("|", header);
                    message += "\r" +String.join("|", Arrays.asList(infos));
                    throw new RuntimeException("CSQ header is not matching the line " + message);
                }
            }
        }
        return features.stream()
                .findFirst()
                .map(TranscriptFeature::getHgvsg)
                .orElse(ctx.getContig() + ':' + ctx.getStart() + "-needs-real-hgvs");
    }


    public TranscriptLevelConsequence  getTranscriptLevelConsq(String[] infos ){
        // Mod VEP
        //  Allele|Consequence|IMPACT|SYMBOL|Gene|Feature_type|Feature|BIOTYPE|EXON|INTRON
        // |HGVSc|HGVSp|cDNA_position|CDS_position|Protein_position|Amino_acids|Codons|Existing_variation|DISTANCE|STRAND|
        //  FLAGS|SYMBOL_SOURCE|HGNC_ID|GIVEN_REF|USED_REF|BAM_EDIT|SOURCE|HGVS_OFFSET|HGVSg|
        //  PolyPhen_prediction|PolyPhen_score|SIFT_prediction|SIFT_score|Genomic_end_position|Genomic_start_position
        TranscriptLevelConsequence c=new TranscriptLevelConsequence();
        c.setTranscriptLevelConsequence( infos[1]);
        c.setImpact( infos[2]);
        Gene g=new Gene();
        g.setSymbol(infos[3]);
        g.setPrimaryKey(infos[4]);
        c.setAssociatedGene(g);
        c.setSequenceFeatureType(infos[7]);
        c.setTranscriptID(infos[6]);
        String location = "";
        if (StringUtils.isNotEmpty(infos[8]))
            location += "Exon " + infos[8];
        if (StringUtils.isNotEmpty(infos[9]))
            location += "Intron " + infos[9];
        c.setTranscriptLocation(location);
        /*  biotype = infos[7];
         */
        c.setCdnaStartPosition(infos[12]);
        c.setCdsStartPosition( infos[13]);
        c.setHgvsCodingNomenclature( infos[10]);
        c.setHgvsProteinNomenclature(infos[11]);
        c.setProteinStartPosition(infos[14]);
        c.setAminoAcidChange(infos[15]);
        c.setCodonChange(infos[16]);
        c.setPolyphenPrediction(infos[30]);
        c.setPolyphenPrediction(infos[30]);
        c.setSiftPrediction(infos[29]);
        c.setSiftScore(infos[32]);
        c.setHgvsVEPGeneNomenclature(infos[28]);

        //  c.setCodonReference(infos[26]); // need to verify

        //  variant.setGenomicVariantSequence(transcriptFeature.getAllele());
        //  genomicEnd = infos[33];
        //    genomicStart = infos[34];

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
